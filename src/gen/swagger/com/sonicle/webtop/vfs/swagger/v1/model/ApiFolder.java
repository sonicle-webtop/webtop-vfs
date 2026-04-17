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



@JsonTypeName("Folder")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-04-17T12:09:51.679+02:00[Europe/Rome]")
public class ApiFolder   {
  private @Valid Integer storeId;
  private @Valid String path;

  /**
   **/
  public ApiFolder storeId(Integer storeId) {
    this.storeId = storeId;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("store_id")
  public Integer getStoreId() {
    return storeId;
  }

  @JsonProperty("store_id")
  public void setStoreId(Integer storeId) {
    this.storeId = storeId;
  }

  /**
   **/
  public ApiFolder path(String path) {
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
    ApiFolder folder = (ApiFolder) o;
    return Objects.equals(this.storeId, folder.storeId) &&
        Objects.equals(this.path, folder.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storeId, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiFolder {\n");
    
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

