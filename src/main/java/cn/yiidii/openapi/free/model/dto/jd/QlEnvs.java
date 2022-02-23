package cn.yiidii.openapi.free.model.dto.jd;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class QlEnvs {


    private String value;
    private String _id;
    private Long created;
    private Integer status;
    private Long position;
    private String name;
    private String remarks;


}
