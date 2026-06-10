import com.mycompany.tutorhub_enterprise.utils.B2Helper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import java.io.File;

public class TestS3 {
    public static void main(String[] args) {
        try {
            System.out.println("Building S3 Client...");
            S3Client s3 = B2Helper.createS3Client();
            
            System.out.println("Creating dummy file...");
            File f = new File("dummy.txt");
            java.nio.file.Files.write(f.toPath(), new byte[1024 * 1024 * 3]); // 3MB file
            
            System.out.println("Uploading...");
            long start = System.currentTimeMillis();
            s3.putObject(PutObjectRequest.builder()
                .bucket(B2Helper.getBucketName())
                .key("test/dummy.txt")
                .build(), RequestBody.fromFile(f));
                
            System.out.println("Uploaded in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
