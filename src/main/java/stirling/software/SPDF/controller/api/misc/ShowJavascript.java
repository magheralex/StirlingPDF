package stirling.software.SPDF.controller.api.misc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;
import stirling.software.SPDF.model.api.PDFFile;
import stirling.software.SPDF.utils.WebResponseUtils;
@RestController
@RequestMapping("/api/v1/misc")
@Tag(name = "Misc", description = "Miscellaneous APIs")
public class ShowJavascript {

    private static final Logger logger = LoggerFactory.getLogger(ShowJavascript.class);
    @PostMapping(consumes = "multipart/form-data", value = "/show-javascript")
    public ResponseEntity<byte[]> extractHeader(@ModelAttribute PDFFile request) throws Exception {
    	MultipartFile inputFile = request.getFileInput();
        String script = "";

        try (PDDocument document = PDDocument.load(inputFile.getInputStream())) {
        	
        	if(document.getDocumentCatalog() != null && document.getDocumentCatalog().getNames() != null) {
	            PDNameTreeNode<PDActionJavaScript> jsTree = document.getDocumentCatalog().getNames().getJavaScript();
	
	            if (jsTree != null) {
	                Map<String, PDActionJavaScript> jsEntries = jsTree.getNames();
	
	                for (Map.Entry<String, PDActionJavaScript> entry : jsEntries.entrySet()) {
	                    String name = entry.getKey();
	                    PDActionJavaScript jsAction = entry.getValue();
	                    String jsCodeStr = jsAction.getAction();
	
	                    script += "// File: " + inputFile.getOriginalFilename() + ", Script: " + name + "\n" + jsCodeStr + "\n";
	                }
	            }
        	}

            if (script.isEmpty()) {
                script = "PDF '" + inputFile.getOriginalFilename() + "' does not contain Javascript";
            }

            return WebResponseUtils.bytesToWebResponse(script.getBytes(StandardCharsets.UTF_8), inputFile.getOriginalFilename() + ".js");
        }
    }
    



}
