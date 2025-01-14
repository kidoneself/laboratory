package cn.yiidii.openapi.free.model.bo.office;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.yiidii.openapi.common.util.Office2Pdf;
import cn.yiidii.openapi.free.component.document.DocumentComponent;
import cn.yiidii.openapi.oss.model.entity.Attachment;
import cn.yiidii.openapi.oss.service.IAttachmentService;
import com.alibaba.fastjson.JSON;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.activation.UnsupportedDataTypeException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * Convert2PdfTask
 *
 * @author YiiDii Wang
 * @create 2021-11-20 11:46
 */
@Data
@Slf4j
@Accessors(chain = true)
@EqualsAndHashCode
public class Convert2PdfTask implements Runnable {

    private String taskId;
    private String callbackUrl;
    private Convert2PdfTaskState state;
    private List<FileInfo> fileInfos;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long consumingTime;

    private DocumentComponent documentComponent;
    private IAttachmentService attachmentService;

    public Convert2PdfTask() {
        super();
        this.taskId = DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN).concat(RandomUtil.randomStringUpper(6));
        this.state = Convert2PdfTaskState.INIT;
        this.startTime = LocalDateTime.now();
    }

    public Convert2PdfTask(List<File> fileList) {
        this();
        List<FileInfo> fileInfos =
                fileList.stream().map(f -> new FileInfo().setFile(f)
                        .setFileName(f.getName()))
                        .collect(Collectors.toList());
        this.setFileInfos(fileInfos);
    }

    public Convert2PdfTask(List<File> fileList, String callbackUrl) {
        this(fileList);
        this.callbackUrl = callbackUrl;
    }

    @Override
    public void run() {
        this.state = Convert2PdfTaskState.RUNNING;

        int failCount = 0;
        for (FileInfo fileInfo : this.fileInfos) {
            fileInfo.setStartTime(LocalDateTime.now());
            fileInfo.setRemark("转换中");
            // 转换
            File resultFile;
            try {
                resultFile = Office2Pdf.convert(fileInfo.getFile());
            } catch (Exception e) {
                log.error("文件[{}]转换失败, e: {}", fileInfo.getFileName(), e.getMessage());
                failCount++;

                fileInfo.setEndTime(LocalDateTime.now());
                fileInfo.setConsumingTime(Duration.between(fileInfo.startTime, fileInfo.endTime).toMillis());

                String message = e.getMessage();
                if (e instanceof UnsupportedDataTypeException) {
                    message = StrUtil.format("不支持的文件类型[{}]", FileUtil.getSuffix(fileInfo.file));
                }
                fileInfo.setRemark(message);
                continue;
            } finally {
                // 删除临时文件
                fileInfo.getFile().delete();
            }

            fileInfo.setRemark("上传中");
            // 上传oss
            Attachment attachment;
            try {
                attachment = this.attachmentService.upload(resultFile);
            } catch (Exception e) {
                log.error("文件[{}]转换失败, e: {}", fileInfo.getFileName(), e.getMessage());
                failCount++;

                fileInfo.setEndTime(LocalDateTime.now());
                fileInfo.setConsumingTime(Duration.between(fileInfo.startTime, fileInfo.endTime).toMillis());
                fileInfo.setRemark("上传至oss失败");
                continue;
            } finally {
                // 删除源文件
                resultFile.delete();
            }

            fileInfo.setAttachment(attachment);
            fileInfo.setRemark("转换成功");
            fileInfo.setEndTime(LocalDateTime.now());
            fileInfo.setConsumingTime(Duration.between(fileInfo.startTime, fileInfo.endTime).toMillis());
            log.info("文件[{}]转换成功", fileInfo.fileName);
        }

        if (failCount == 0) {
            this.setState(Convert2PdfTaskState.SUCCESS);
        } else if (failCount == fileInfos.size()) {
            this.setState(Convert2PdfTaskState.FAIL);
        } else {
            this.setState(Convert2PdfTaskState.PART_SUCCESS);
        }
        this.endTime = LocalDateTime.now();
        this.consumingTime = Duration.between(this.startTime, this.endTime).toMillis();

        // callback
        if (StrUtil.isNotBlank(callbackUrl)) {
            List<Attachment> attachments = this.getFileInfos().stream().map(FileInfo::getAttachment).collect(Collectors.toList());
            HttpRequest.post(callbackUrl).body(JSON.toJSONString(attachments)).execute();
        }

    }

    @Data
    @Slf4j
    @Accessors(chain = true)
    @EqualsAndHashCode
    protected static class FileInfo {

        private File file;
        private String fileName;
        private Attachment attachment;
        private String remark = "暂无信息";

        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long consumingTime;
    }
}
