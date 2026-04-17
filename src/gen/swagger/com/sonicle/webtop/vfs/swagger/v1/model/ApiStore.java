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



@JsonTypeName("Store")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-04-17T12:09:51.679+02:00[Europe/Rome]")
public class ApiStore   {
  private @Valid Integer id;
  private @Valid String name;
  private @Valid String provider;
  private @Valid Boolean incoming;

  /**
   **/
  public ApiStore id(Integer id) {
    this.id = id;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("id")
  public Integer getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   **/
  public ApiStore name(String name) {
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
  public ApiStore provider(String provider) {
    this.provider = provider;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("provider")
  public String getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(String provider) {
    this.provider = provider;
  }

  /**
   **/
  public ApiStore incoming(Boolean incoming) {
    this.incoming = incoming;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("incoming")
  public Boolean getIncoming() {
    return incoming;
  }

  @JsonProperty("incoming")
  public void setIncoming(Boolean incoming) {
    this.incoming = incoming;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiStore store = (ApiStore) o;
    return Objects.equals(this.id, store.id) &&
        Objects.equals(this.name, store.name) &&
        Objects.equals(this.provider, store.provider) &&
        Objects.equals(this.incoming, store.incoming);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, provider, incoming);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiStore {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    incoming: ").append(toIndentedString(incoming)).append("\n");
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

