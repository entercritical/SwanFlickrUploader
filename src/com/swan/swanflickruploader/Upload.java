package com.swan.swanflickruploader;

import java.io.File;
import java.io.FileInputStream;
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

        // 일단 사용자가 지정한 directory 밑에는 디렉토리들 존재하고
        // 디렉토리 명을 세트명으로 하고 *.jpg 등 이미지 파일을 올리는 경우만 고려한다.
        File[] contents = directory.listFiles();

        Uploader uploader = flickr.getUploader();
        PhotosInterface pint = flickr.getPhotosInterface();
        InputStream in = null;

        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                System.out.println("<<" + contents[i].getName() + ">>");
                File[] list = contents[i].listFiles(new ImageFilenameFilter());

                for (int j = 0; j < list.length; j++) {
                    if (list[j].isFile()) {
                        System.out.println("Uploading : " + list[j].getName());

                        try {
                            in = new FileInputStream(list[j]);
                            UploadMetaData metaData = new UploadMetaData();
                            metaData.setPublicFlag(false);
                            metaData.setHidden(true);

                            // check correct handling of escaped value
                            metaData.setTitle(list[j].getName());
                            String photoId = uploader.upload(in, metaData);

                            // assertNotNull(photoId);
                            // pint.delete(photoId);
                        } finally {
                            IOUtilities.close(in);
                        }

                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out
                    .println("Usage: java -jar SwanFlickrUploader target_dir");
            System.exit(1);
        }

        Upload upload = new Upload(new File(System.getProperty("user.home")
                + File.separatorChar + ".flickrAuth"));

        upload.upload(new File(args[0]));
    }
}
