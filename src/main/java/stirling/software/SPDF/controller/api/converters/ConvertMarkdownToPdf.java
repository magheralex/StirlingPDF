package stirling.software.SPDF.controller.api.converters;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import stirling.software.SPDF.model.api.GeneralFile;
import stirling.software.SPDF.utils.FileToPdf;
import stirling.software.SPDF.utils.WebResponseUtils;

@RestController
@Tag(name = "Convert", description = "Convert APIs")
@RequestMapping("/api/v1/convert")
public class ConvertMarkdownToPdf {
	
	@PostMapping(consumes = "multipart/form-data", value = "/markdown/pdf")
    @Operation(
        summary = "Convert a Markdown file to PDF",
        description = "This endpoint takes a Markdown file input, converts it to HTML, and then to PDF format."
    )
    public ResponseEntity<byte[]> markdownToPdf(
    		@ModelAttribute GeneralFile request) 
    		        throws Exception {
    			MultipartFile fileInput = request.getFileInput();
        
        if (fileInput == null) {
            throw new IllegalArgumentException("Please provide a Markdown file for conversion.");
        }

        String originalFilename = fileInput.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".md")) {
            throw new IllegalArgumentException("File must be in .md format.");
        }

        // Convert Markdown to HTML using CommonMark
        Parser parser = Parser.builder().build();
        Node document = parser.parse(new String(fileInput.getBytes()));
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String htmlContent = renderer.render(document);
        
        byte[] pdfBytes = FileToPdf.convertHtmlToPdf(htmlContent.getBytes(), "converted.html"); 

        String outputFilename = originalFilename.replaceFirst("[.][^.]+$", "") + ".pdf";  // Remove file extension and append .pdf
        return WebResponseUtils.bytesToWebResponse(pdfBytes, outputFilename);
    }
}
