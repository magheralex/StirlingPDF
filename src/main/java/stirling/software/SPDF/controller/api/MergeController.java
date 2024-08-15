package stirling.software.SPDF.controller.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import stirling.software.SPDF.model.api.general.MergePdfsRequest;
import stirling.software.SPDF.utils.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/general")
@Tag(name = "General", description = "General APIs")
public class MergeController {

    private static final Logger logger = LoggerFactory.getLogger(MergeController.class);


private PDDocument mergeDocuments(List<PDDocument> documents) throws IOException {
	PDDocument mergedDoc = new PDDocument();
    for (PDDocument doc : documents) {
        for (PDPage page : doc.getPages()) {
            mergedDoc.addPage(page);
        }
    }
    return mergedDoc;
}

private Comparator<MultipartFile> getSortComparator(String sortType) {
    switch (sortType) {
        case "byFileName":
            return Comparator.comparing(MultipartFile::getOriginalFilename);
        case "byDateModified":
            return (file1, file2) -> {
                try {
                    BasicFileAttributes attr1 = Files.readAttributes(Paths.get(file1.getOriginalFilename()), BasicFileAttributes.class);
                    BasicFileAttributes attr2 = Files.readAttributes(Paths.get(file2.getOriginalFilename()), BasicFileAttributes.class);
                    return attr1.lastModifiedTime().compareTo(attr2.lastModifiedTime());
                } catch (IOException e) {
                    return 0;  // If there's an error, treat them as equal
                }
            };
        case "byDateCreated":
            return (file1, file2) -> {
                try {
                    BasicFileAttributes attr1 = Files.readAttributes(Paths.get(file1.getOriginalFilename()), BasicFileAttributes.class);
                    BasicFileAttributes attr2 = Files.readAttributes(Paths.get(file2.getOriginalFilename()), BasicFileAttributes.class);
                    return attr1.creationTime().compareTo(attr2.creationTime());
                } catch (IOException e) {
                    return 0;  // If there's an error, treat them as equal
                }
            };
        case "byPDFTitle":
            return (file1, file2) -> {
                try (PDDocument doc1 = PDDocument.load(file1.getInputStream());
                     PDDocument doc2 = PDDocument.load(file2.getInputStream())) {
                    String title1 = doc1.getDocumentInformation().getTitle();
                    String title2 = doc2.getDocumentInformation().getTitle();
                    return title1.compareTo(title2);
                } catch (IOException e) {
                    return 0;
                }
            };
        case "orderProvided":
        default:
            return (file1, file2) -> 0;  // Default is the order provided
    }
}

@PostMapping(consumes = "multipart/form-data", value = "/merge-pdfs")
@Operation(summary = "Merge multiple PDF files into one",
        description = "This endpoint merges multiple PDF files into a single PDF file. The merged file will contain all pages from the input files in the order they were provided. Input:PDF Output:PDF Type:MISO")
public ResponseEntity<byte[]> mergePdfs(@ModelAttribute MergePdfsRequest form) throws IOException {

	MultipartFile[]  files = form.getFileInput();
    Arrays.sort(files, getSortComparator(form.getSortType()));

    List<PDDocument> documents = new ArrayList<>();
    for (MultipartFile file : files) {
        try (InputStream is = file.getInputStream()) {
            documents.add(PDDocument.load(is));
        }
    }

    try (PDDocument mergedDoc = mergeDocuments(documents)) {
        ResponseEntity<byte[]> response = WebResponseUtils.pdfDocToWebResponse(mergedDoc, files[0].getOriginalFilename().replaceFirst("[.][^.]+$", "") + "_merged.pdf");
        return response;
    } finally {
        for (PDDocument doc : documents) {
            if (doc != null) {
                doc.close();
            }
        }
    }
}

}