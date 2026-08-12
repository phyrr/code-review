package com.phyrr.codereview.domain.model;

public enum Model {

    GLM_4_FLASH("glm-4-flash", "简单任务，速度快，128k 上下文"),
    GLM_4("glm-4", "复杂对话与深度内容创作"),
    GLM_3_5_TURBO("glm-3-turbo", "兼顾效果与成本"),
    ;

    private final String code;
    private final String info;

    Model(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

}
