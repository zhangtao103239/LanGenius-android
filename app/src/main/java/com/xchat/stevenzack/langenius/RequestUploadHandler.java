package com.xchat.stevenzack.langenius;

import android.os.Environment;
import android.os.Message;

import com.yanzhenjie.andserver.RequestHandler;
import com.yanzhenjie.andserver.upload.HttpFileUpload;
import com.yanzhenjie.andserver.upload.HttpUploadContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RequestUploadHandler implements RequestHandler {
    public static File saveDirectory= Environment.getExternalStorageDirectory();
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

        if (!HttpFileUpload.isMultipartContentWithPost(request)) { // Is POST and upload.
            response(403, "You must upload file.", response);
        } else {
            if (saveDirectory.isDirectory()) {
                try {
                    processFileUpload(request, saveDirectory);
                    response(200, "Ok.", response);
                } catch (Exception e) {
                    e.printStackTrace();
                    response(500, "Save the file when the error occurs.", response);
                }
            } else {
                response(500, "The server can not save the file.", response);
            }
        }
    }

    private void response(int responseCode, String message, HttpResponse response) throws IOException {
        response.setStatusCode(responseCode);
        response.setEntity(new StringEntity(message, "utf-8"));
    }
    private void processFileUpload(HttpRequest request, File saveDirectory) throws Exception {
        FileItemFactory factory = new DiskFileItemFactory(1024 * 1024, saveDirectory);
        HttpFileUpload fileUpload = new HttpFileUpload(factory);
        List<FileItem> fileItems = fileUpload.parseRequest(new HttpUploadContext((HttpEntityEnclosingRequest) request));
        for (FileItem fileItem : fileItems) {
            if (!fileItem.isFormField()) { // File param.
                File uploadedFile = new File(saveDirectory, fileItem.getName());
                fileItem.write(uploadedFile);
                Message msg=new Message();
                msg.arg1=1;
                msg.obj=fileItem.getName();
                if (HandlerConverter.MainActivity_handler!=null){
                    HandlerConverter.MainActivity_handler.sendMessage(msg);
                }
            } else {
                String key = fileItem.getName();
                String value = fileItem.getString();
            }
        }

    }
}