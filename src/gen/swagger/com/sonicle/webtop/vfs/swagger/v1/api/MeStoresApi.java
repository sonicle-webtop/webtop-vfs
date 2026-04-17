package com.sonicle.webtop.vfs.swagger.v1.api;

import com.sonicle.webtop.vfs.swagger.v1.model.ApiFile;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiFileUpload;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiFolder;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;

import java.io.InputStream;
import java.util.Map;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;

@Path("/me")
@Api(description = "the MeStores API")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", date = "2026-04-17T12:09:51.679+02:00[Europe/Rome]")
public abstract class MeStoresApi extends com.sonicle.webtop.core.sdk.BaseRestApiResource {

    @GET
    @Path("/files/$value")
    @Consumes({ "application/json" })
    @Produces({ "application/octet-stream" })
    @ApiOperation(value = "Get file content of store path", notes = "Get File Content", response = Object.class, authorizations = {
        
        @Authorization(value = "basicAuth"),
        
        @Authorization(value = "bearerAuth")
         }, tags={ "me_stores" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = Object.class)
    })
    public Response getFileContent(@Valid ApiFile apiFile) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/files")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Lists files of store path", notes = "List Files", response = ApiFile.class, responseContainer = "List", authorizations = {
        
        @Authorization(value = "basicAuth"),
        
        @Authorization(value = "bearerAuth")
         }, tags={ "me_stores" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = ApiFile.class, responseContainer = "List")
    })
    public Response listFiles(@Valid ApiFolder apiFolder) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/folders")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Lists folders of store path", notes = "List Folders", response = ApiFolder.class, responseContainer = "List", authorizations = {
        
        @Authorization(value = "basicAuth"),
        
        @Authorization(value = "bearerAuth")
         }, tags={ "me_stores" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = ApiFolder.class, responseContainer = "List")
    })
    public Response listFolders(@Valid ApiFolder apiFolder) {
        return Response.ok().entity("magic!").build();
    }

    @GET
    @Path("/stores")
    @Produces({ "application/json" })
    @ApiOperation(value = "Lists stores", notes = "", response = ApiStore.class, responseContainer = "List", authorizations = {
        
        @Authorization(value = "basicAuth"),
        
        @Authorization(value = "bearerAuth")
         }, tags={ "me_stores" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "Success", response = ApiStore.class, responseContainer = "List")
    })
    public Response listStores() {
        return Response.ok().entity("magic!").build();
    }

    @POST
    @Path("/files/$value")
    @Consumes({ "application/json" })
    @ApiOperation(value = "Upload file content of store path", notes = "", response = Void.class, authorizations = {
        
        @Authorization(value = "basicAuth"),
        
        @Authorization(value = "bearerAuth")
         }, tags={ "me_stores" })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK", response = Void.class)
    })
    public Response uploadFileContent(@Valid ApiFileUpload apiFileUpload) {
        return Response.ok().entity("magic!").build();
    }
}
