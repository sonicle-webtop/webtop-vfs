package com.sonicle.webtop.vfs.swagger.v1.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.*;
import javax.validation.Valid;

import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonTypeName;



@JsonTypeName("File")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-04-17T12:09:51.679+02:00[Europe/Rome]")
public class ApiFile   {
  private @Valid String name;
  private @Valid String contentType;
  private @Valid Long size;
  private @Valid String lastModified;
  private @Valid Integer storeId;
  private @Valid String path;

  /**
   **/
  public ApiFile name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/
  public ApiFile contentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("contentType")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("contentType")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   **/
  public ApiFile size(Long size) {
    this.size = size;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("size")
  public Long getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(Long size) {
    this.size = size;
  }

  /**
   **/
  public ApiFile lastModified(String lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("lastModified")
  public String getLastModified() {
    return lastModified;
  }

  @JsonProperty("lastModified")
  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  /**
   **/
  public ApiFile storeId(Integer storeId) {
    this.storeId = storeId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("storeId")
  public Integer getStoreId() {
    return storeId;
  }

  @JsonProperty("storeId")
  public void setStoreId(Integer storeId) {
    this.storeId = storeId;
  }

  /**
   **/
  public ApiFile path(String path) {
    this.path = path;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiFile _file = (ApiFile) o;
    return Objects.equals(this.name, _file.name) &&
        Objects.equals(this.contentType, _file.contentType) &&
        Objects.equals(this.size, _file.size) &&
        Objects.equals(this.lastModified, _file.lastModified) &&
        Objects.equals(this.storeId, _file.storeId) &&
        Objects.equals(this.path, _file.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, contentType, size, lastModified, storeId, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiFile {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    contentType: ").append(toIndentedString(contentType)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    lastModified: ").append(toIndentedString(lastModified)).append("\n");
    sb.append("    storeId: ").append(toIndentedString(storeId)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }


}

