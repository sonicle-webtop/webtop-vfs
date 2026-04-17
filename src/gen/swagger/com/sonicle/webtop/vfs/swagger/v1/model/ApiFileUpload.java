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



@JsonTypeName("FileUpload")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-04-17T12:09:51.679+02:00[Europe/Rome]")
public class ApiFileUpload   {
  private @Valid String name;
  private @Valid Integer storeId;
  private @Valid String path;
  private @Valid String base64;

  /**
   **/
  public ApiFileUpload name(String name) {
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
  public ApiFileUpload storeId(Integer storeId) {
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
  public ApiFileUpload path(String path) {
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

  /**
   **/
  public ApiFileUpload base64(String base64) {
    this.base64 = base64;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("base64")
  public String getBase64() {
    return base64;
  }

  @JsonProperty("base64")
  public void setBase64(String base64) {
    this.base64 = base64;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiFileUpload fileUpload = (ApiFileUpload) o;
    return Objects.equals(this.name, fileUpload.name) &&
        Objects.equals(this.storeId, fileUpload.storeId) &&
        Objects.equals(this.path, fileUpload.path) &&
        Objects.equals(this.base64, fileUpload.base64);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, storeId, path, base64);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiFileUpload {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    storeId: ").append(toIndentedString(storeId)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    base64: ").append(toIndentedString(base64)).append("\n");
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

