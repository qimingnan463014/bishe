package com.salary.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ExcelAvatarExportUtil {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int DEFAULT_ROW_HEIGHT = 22;
    private static final int AVATAR_ROW_HEIGHT = 48;

    private ExcelAvatarExportUtil() {
    }

    public static <T> ExportColumn<T> text(String header, int width, Function<T, ?> valueProvider) {
        return new ExportColumn<>(header, width, false, valueProvider);
    }

    public static <T> ExportColumn<T> avatar(String header, int width, Function<T, ?> valueProvider) {
        return new ExportColumn<>(header, width, true, valueProvider);
    }

    public static <T> void export(HttpServletResponse response,
                                  String fileName,
                                  String sheetName,
                                  List<ExportColumn<T>> columns,
                                  List<T> rows) {
        response.setContentType(CONTENT_TYPE);
        try {
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("设置导出响应头失败", e);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream outputStream = response.getOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet =
                    workbook.createSheet(WorkbookUtil.createSafeSheetName(sheetName));
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle textStyle = createTextStyle(workbook);
            CellStyle avatarCellStyle = createAvatarCellStyle(workbook);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(DEFAULT_ROW_HEIGHT);
            for (int i = 0; i < columns.size(); i++) {
                ExportColumn<T> column = columns.get(i);
                sheet.setColumnWidth(i, Math.max(8, column.width) * 256);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.header);
                cell.setCellStyle(headerStyle);
            }

            org.apache.poi.ss.usermodel.Drawing<?> drawing = sheet.createDrawingPatriarch();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                T item = rows.get(rowIndex);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
                row.setHeightInPoints(hasAvatarColumn(columns) ? AVATAR_ROW_HEIGHT : DEFAULT_ROW_HEIGHT);
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    ExportColumn<T> column = columns.get(colIndex);
                    Cell cell = row.createCell(colIndex);
                    if (column.avatar) {
                        cell.setCellStyle(avatarCellStyle);
                        String avatarPath = stringify(column.valueProvider.apply(item));
                        byte[] imageBytes = readImageBytes(avatarPath);
                        if (imageBytes != null) {
                            int pictureType = detectPictureType(avatarPath, imageBytes);
                            int pictureIndex = workbook.addPicture(imageBytes, pictureType);
                            org.apache.poi.ss.usermodel.ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                            anchor.setCol1(colIndex);
                            anchor.setRow1(rowIndex + 1);
                            anchor.setCol2(colIndex + 1);
                            anchor.setRow2(rowIndex + 2);
                            drawing.createPicture(anchor, pictureIndex);
                        } else {
                            cell.setCellValue("-");
                        }
                    } else {
                        cell.setCellValue(stringify(column.valueProvider.apply(item)));
                        cell.setCellStyle(textStyle);
                    }
                }
            }

            sheet.createFreezePane(0, 1);
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("导出 Excel 失败：" + e.getMessage(), e);
        }
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    public static String formatNumber(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            java.math.BigDecimal decimal = new java.math.BigDecimal(text);
            return decimal.stripTrailingZeros().toPlainString();
        } catch (Exception ignored) {
            return text;
        }
    }

    public static String formatDate(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    public static String fileNameOf(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path.replace("\\", "/");
        int index = normalized.lastIndexOf('/');
        return index >= 0 ? normalized.substring(index + 1) : normalized;
    }

    private static boolean hasAvatarColumn(List<? extends ExportColumn<?>> columns) {
        for (ExportColumn<?> column : columns) {
            if (column.avatar) {
                return true;
            }
        }
        return false;
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle createTextStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle createAvatarCellStyle(XSSFWorkbook workbook) {
        CellStyle style = createTextStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static byte[] readImageBytes(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        String path = rawPath.trim();
        if (path.startsWith("data:image")) {
            int commaIndex = path.indexOf(',');
            if (commaIndex > 0) {
                try {
                    return Base64.getDecoder().decode(path.substring(commaIndex + 1));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }

        String normalized = normalizeAvatarPath(path);
        List<Path> candidates = new ArrayList<>();
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Paths.get(normalized).isAbsolute()) {
            candidates.add(Paths.get(normalized));
        } else if (normalized.startsWith("uploads/")) {
            candidates.add(projectRoot.resolve(normalized));
        } else if (normalized.startsWith("front_assets/")) {
            candidates.add(projectRoot.resolve("src/main/resources/static").resolve(normalized));
        } else {
            candidates.add(projectRoot.resolve(normalized));
            candidates.add(projectRoot.resolve("src/main/resources/static").resolve(normalized));
        }

        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate)) {
                try {
                    return Files.readAllBytes(candidate);
                } catch (IOException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String normalizeAvatarPath(String avatarPath) {
        String normalized = avatarPath.replace('\\', '/');
        int apiIndex = normalized.indexOf("/api/");
        if (apiIndex >= 0) {
            normalized = normalized.substring(apiIndex + 5);
        }
        int uploadsIndex = normalized.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            normalized = normalized.substring(uploadsIndex + 1);
        }
        int frontAssetsIndex = normalized.indexOf("/front_assets/");
        if (frontAssetsIndex >= 0) {
            normalized = normalized.substring(frontAssetsIndex + 1);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static int detectPictureType(String avatarPath, byte[] bytes) {
        String lowerPath = Objects.toString(avatarPath, "").toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        if (bytes != null && bytes.length >= 8) {
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return Workbook.PICTURE_TYPE_PNG;
            }
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
                return Workbook.PICTURE_TYPE_JPEG;
            }
        }
        return Workbook.PICTURE_TYPE_PNG;
    }

    public static final class ExportColumn<T> {
        private final String header;
        private final int width;
        private final boolean avatar;
        private final Function<T, ?> valueProvider;

        private ExportColumn(String header, int width, boolean avatar, Function<T, ?> valueProvider) {
            this.header = header;
            this.width = width;
            this.avatar = avatar;
            this.valueProvider = valueProvider;
        }
    }
}
