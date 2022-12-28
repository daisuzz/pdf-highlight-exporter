package dev.daisuzz;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {
    public static void main(String[] args) throws IOException {
        try (PDDocument doc = PDDocument.load(new File(args[0]))) {
            PDPageTree allPage = doc.getPages();
            for (PDPage page : allPage) {
                int pageNumber = allPage.indexOf(page) - 1;
                for (PDAnnotation annotation : page.getAnnotations()) {
                    if (annotation.getSubtype().equals("Highlight")) {
                        List<List<? extends COSBase>> quadPointLists = extractQuadPointLists(annotation);
                        String extractedHighLight = extractHighLight(page, quadPointLists);
                        System.out.printf("%s page,%s\n", pageNumber, extractedHighLight.replaceAll("\n", ""));
                    }
                }
            }
        }
    }

    static List<List<? extends COSBase>> extractQuadPointLists(PDAnnotation annotation) {
        // QuadPointsを取得
        // QuadPointsとは各文字が埋め込まれている矩形の四隅の座標を指す
        COSDictionary cosDictionary = annotation.getCOSObject();
        COSArray cosArray = cosDictionary.getCOSArray(COSName.QUADPOINTS);

        // 座標を表す数値を8個ずつに切り分けてリストに追加
        List<List<? extends COSBase>> quadPointLists = new ArrayList<>();
        for (int i = 0; i < cosArray.size(); i += 8) {
            quadPointLists.add(cosArray.toList().subList(i, i + 8));
        }

        return quadPointLists;
    }

    private static String extractHighLight(final PDPage page, final List<List<? extends COSBase>> quadPointLists) throws IOException {
        PDFTextStripperByArea pdfTextStripperByArea = new PDFTextStripperByArea();
        StringBuilder extractedText = new StringBuilder();
        for (List<? extends COSBase> quadPoints : quadPointLists) {
            float width = ((COSNumber) quadPoints.get(2)).floatValue() - ((COSNumber) quadPoints.get(4)).floatValue();
            float height = ((COSNumber) quadPoints.get(3)).floatValue() - ((COSNumber) quadPoints.get(5)).floatValue();
            pdfTextStripperByArea.addRegion("highlighted region",
                    new Rectangle2D.Float(
                            ((COSNumber) quadPoints.get(0)).floatValue() - 1,
                            page.getMediaBox().getHeight() - ((COSNumber) quadPoints.get(1)).floatValue(),
                            width,
                            height)
            );
            pdfTextStripperByArea.extractRegions(page);
            String text = pdfTextStripperByArea.getTextForRegion("highlighted region");
            extractedText.append(text);
        }
        return extractedText.toString();
    }
}
