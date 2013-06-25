package com.swan.swanflickruploader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.xml.sax.SAXException;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;
import com.flickr4java.flickr.util.IOUtilities;

public class Upload {
    private String nsid;

    private Flickr flickr;

    private AuthStore authStore;

    private void authorize() throws IOException, SAXException, FlickrException {
        AuthInterface authInterface = flickr.getAuthInterface();
        Token accessToken = authInterface.getRequestToken();

        String url = authInterface.getAuthorizationUrl(accessToken,
                Permission.DELETE);
        System.out.println("Follow this URL to authorise yourself on Flickr");
        System.out.println(url);
        System.out.println("Paste in the token it gives you:");
        System.out.print(">>");

        String tokenKey = new Scanner(System.in).nextLine();

        Token requestToken = authInterface.getAccessToken(accessToken,
                new Verifier(tokenKey));

        Auth auth = authInterface.checkToken(requestToken);
        RequestContext.getRequestContext().setAuth(auth);
        this.authStore.store(auth);
        System.out
                .println("Thanks.  You probably will not have to do this every time.");
    }

    public Upload(String apiKey, String secret, String nsid, File authsDir) throws FlickrException, IOException {
        flickr = new Flickr(apiKey, secret, new REST());
        this.nsid = nsid;

        if (authsDir != null) {
            this.authStore = new FileAuthStore(authsDir);
        }       
    }
    
    public Upload(File authsDir) throws FlickrException, IOException {
        Properties properties;
        InputStream in = null;
        try {
            in = Upload.class.getResourceAsStream("setup.properties");
            properties = new Properties();
            properties.load(in);
        } finally {
            IOUtilities.close(in);
        }

        flickr = new Flickr(properties.getProperty("apiKey"),
                properties.getProperty("secret"), new REST());
        this.nsid = properties.getProperty("nsid");

        if (authsDir != null) {
            this.authStore = new FileAuthStore(authsDir);
        }
    }

    private void uploadFiles(String name, File[] fileList) {
        InputStream in = null;
        Uploader uploader = flickr.getUploader();
        PhotosetsInterface psint = flickr.getPhotosetsInterface();
        Photoset photoset = null; // psint.create(name, null, null);
        System.out.println("# New Set : " + name);
        
        for (int j = 0; j < fileList.length; j++) {
            if (fileList[j].isFile()) {
                System.out.println("  Uploading : " + fileList[j].getName());

                try {
                    in = new FileInputStream(fileList[j]);
                    UploadMetaData metaData = new UploadMetaData();
                    metaData.setPublicFlag(false);
                    metaData.setHidden(true);

                    // check correct handling of escaped value
                    metaData.setTitle(fileList[j].getName());
                    String photoId = uploader.upload(in, metaData);

                    // add to Photoset
                    if (name != null) {
                        if (photoset == null) {
                            photoset = psint.create(name, "", photoId);
                        } else {
                            psint.addPhoto(photoset.getId(), photoId);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (FlickrException e) {
                    e.printStackTrace();
                } finally {
                    IOUtilities.close(in);
                }

            }
        }
    }
    
    public void upload(File directory) throws IOException, SAXException,
            FlickrException {
        RequestContext rc = RequestContext.getRequestContext();

        if (this.authStore != null) {
            Auth auth = this.authStore.retrieve(this.nsid);
            if (auth == null) {
                this.authorize();
            } else {
                rc.setAuth(auth);
            }
        }
        
        File[] contents = directory.listFiles();

        PhotosetsInterface psint = flickr.getPhotosetsInterface();

        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                File[] fileList = contents[i].listFiles(new ImageFilenameFilter());
                uploadFiles(contents[i].getName(), fileList);
            } else {
                
                ImageFilenameFilter filter = new ImageFilenameFilter();
                if (filter.accept(null, contents[i].getName())) {
                    System.out.println("* Not in a set *");
                    File[] fileList = {contents[i]};
                    uploadFiles(null, fileList);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out
                    .println("Usage: java -jar SwanFlickrUploader.jar api_key secret nsid target_dir");
            System.exit(1);
        }

        Upload upload = new Upload(
                args[0],
                args[1],
                args[2],
                new File(System.getProperty("user.home") + File.separatorChar + ".flickrAuth"));

        upload.upload(new File(args[3]));
    }
}
