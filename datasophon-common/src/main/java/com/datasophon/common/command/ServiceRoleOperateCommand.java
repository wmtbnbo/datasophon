package com.datasophon.common.command;

import com.datasophon.common.enums.CommandType;
import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceRoleOperateCommand extends BaseCommand implements Serializable {

    private static final long serialVersionUID = 6454341380133032878L;
    private Integer serviceRoleInstanceId;

    private CommandType commandType;

    private Long deliveryId;

    private String decompressPackageName;

    private boolean isSlave;

    private String masterHost;

    private Boolean enableRangerPlugin;

    private String runAs;

}
