package stirling.software.SPDF.controller.api.converters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import stirling.software.SPDF.model.api.GeneralFile;
import stirling.software.SPDF.utils.ProcessExecutor;
import stirling.software.SPDF.utils.ProcessExecutor.ProcessExecutorResult;
import stirling.software.SPDF.utils.WebResponseUtils;

@RestController
@Tag(name = "Convert", description = "Convert APIs")
@RequestMapping("/api/v1/convert")
public class ConvertOfficeController {

    public byte[] convertToPdf(MultipartFile inputFile) throws IOException, InterruptedException {
        // Check for valid file extension
        String originalFilename = inputFile.getOriginalFilename();
        if (originalFilename == null || !isValidFileExtension(FilenameUtils.getExtension(originalFilename))) {
            throw new IllegalArgumentException("Invalid file extension");
        }

        // Save the uploaded file to a temporary location
        Path tempInputFile = Files.createTempFile("input_", "." + FilenameUtils.getExtension(originalFilename));
        Files.copy(inputFile.getInputStream(), tempInputFile, StandardCopyOption.REPLACE_EXISTING);

        // Prepare the output file path
        Path tempOutputFile = Files.createTempFile("output_", ".pdf");

        // Run the LibreOffice command
        List<String> command = new ArrayList<>(Arrays.asList("unoconv", "-vvv", "-f", "pdf", "-o", tempOutputFile.toString(), tempInputFile.toString()));
        ProcessExecutorResult returnCode = ProcessExecutor.getInstance(ProcessExecutor.Processes.LIBRE_OFFICE).runCommandWithOutputHandling(command);

        // Read the converted PDF file
        byte[] pdfBytes = Files.readAllBytes(tempOutputFile);

        // Clean up the temporary files
        Files.delete(tempInputFile);
        Files.delete(tempOutputFile);

        return pdfBytes;
    }
    private boolean isValidFileExtension(String fileExtension) {
        String extensionPattern = "^(?i)[a-z0-9]{2,4}$";
        return fileExtension.matches(extensionPattern);
    }

    @PostMapping(consumes = "multipart/form-data", value = "/file/pdf")
    @Operation(
        summary = "Convert a file to a PDF using LibreOffice",
        description = "This endpoint converts a given file to a PDF using LibreOffice API  Input:Any Output:PDF Type:SISO"
    )
    public ResponseEntity<byte[]> processFileToPDF(@ModelAttribute GeneralFile request) 
	        throws Exception {
		MultipartFile inputFile = request.getFileInput();
        // unused but can start server instance if startup time is to long
        // LibreOfficeListener.getInstance().start();

        byte[] pdfByteArray = convertToPdf(inputFile);
        return WebResponseUtils.bytesToWebResponse(pdfByteArray, inputFile.getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_convertedToPDF.pdf");
    }

}
