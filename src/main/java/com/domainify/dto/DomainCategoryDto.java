package com.domainify.dto;

import java.util.ArrayList;
import java.util.List;

public class DomainCategoryDto {

    private Long id;
    private String code;
    private String name;
    private Long parentId;
    private String parentName;
    private boolean active;
    private int sortOrder;
    private int depth;
    private List<DomainCategoryDto> children = new ArrayList<>();

    public DomainCategoryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<DomainCategoryDto> getChildren() {
        return children;
    }

    public void setChildren(List<DomainCategoryDto> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
}
