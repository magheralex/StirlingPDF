package stirling.software.SPDF.controller.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;

import stirling.software.SPDF.model.api.PDFFile;
import stirling.software.SPDF.service.PdfImageRemovalService;
import stirling.software.SPDF.utils.WebResponseUtils;

/**
 * Controller class for handling PDF image removal requests. Provides an endpoint to remove images
 * from a PDF file to reduce its size.
 */
@RestController
@RequestMapping("/api/v1/general")
public class PdfImageRemovalController {

    // Service for removing images from PDFs
    @Autowired private PdfImageRemovalService pdfImageRemovalService;

    /**
     * Constructor for dependency injection of PdfImageRemovalService.
     *
     * @param pdfImageRemovalService The service used for removing images from PDFs.
     */
    public PdfImageRemovalController(PdfImageRemovalService pdfImageRemovalService) {
        this.pdfImageRemovalService = pdfImageRemovalService;
    }

    /**
     * Endpoint to remove images from a PDF file.
     *
     * <p>This method processes the uploaded PDF file, removes all images, and returns the modified
     * PDF file with a new name indicating that images were removed.
     *
     * @param file The PDF file with images to be removed.
     * @return ResponseEntity containing the modified PDF file as byte array with appropriate
     *     content type and filename.
     * @throws IOException If an error occurs while processing the PDF file.
     */
    @PostMapping(consumes = "multipart/form-data", value = "/remove-image-pdf")
    @Operation(
            summary = "Remove images from file to reduce the file size.",
            description =
                    "This endpoint remove images from file to reduce the file size.Input:PDF Output:PDF Type:MISO")
    public ResponseEntity<byte[]> removeImages(@ModelAttribute PDFFile file) throws IOException {

        MultipartFile pdf = file.getFileInput();

        // Convert the MultipartFile to a byte array
        byte[] pdfBytes = pdf.getBytes();

        // Load the PDF document from the byte array
        PDDocument document = Loader.loadPDF(pdfBytes);

        // Remove images from the PDF document using the service
        PDDocument modifiedDocument = pdfImageRemovalService.removeImagesFromPdf(document);

        // Create a ByteArrayOutputStream to hold the modified PDF data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Save the modified PDF document to the output stream
        modifiedDocument.save(outputStream);
        modifiedDocument.close();

        // Generate a new filename for the modified PDF
        String mergedFileName =
                pdf.getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_removed_images.pdf";

        // Convert the byte array to a web response and return it
        return WebResponseUtils.bytesToWebResponse(outputStream.toByteArray(), mergedFileName);
    }
}
