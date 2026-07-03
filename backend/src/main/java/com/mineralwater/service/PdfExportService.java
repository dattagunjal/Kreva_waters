package com.mineralwater.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final SalesService salesService;

    // Brand colors
    private static final DeviceRgb PRIMARY      = new DeviceRgb(26, 107, 191);
    private static final DeviceRgb PRIMARY_LIGHT = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb SUCCESS      = new DeviceRgb(21, 128, 61);
    private static final DeviceRgb SUCCESS_LIGHT = new DeviceRgb(220, 252, 231);
    private static final DeviceRgb DANGER       = new DeviceRgb(153, 27, 27);
    private static final DeviceRgb DANGER_LIGHT  = new DeviceRgb(254, 226, 226);
    private static final DeviceRgb WARNING_LIGHT = new DeviceRgb(254, 249, 195);
    private static final DeviceRgb MUTED        = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT_BG     = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDER       = new DeviceRgb(226, 232, 240);

    /**
     * Export Daily Sales PDF
     */
    public byte[] exportDailySalesPdf(LocalDate date) {
        Map<String, Object> data = salesService.getDailySales(date);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            addHeader(doc, "Daily Sales Report", "Date: " + date.toString());
            addDivider(doc);
            addSummaryCards(doc, data);
            addDivider(doc);
            addTopProductsTable(doc, (List<Map<String, Object>>) data.get("topProducts"));
            addDivider(doc);
            addOrdersTable(doc, (List<Map<String, Object>>) data.get("orders"));
            addFooter(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate daily PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    /**
     * Export Monthly Sales PDF
     */
    public byte[] exportMonthlySalesPdf(int year, int month) {
        Map<String, Object> data = salesService.getMonthlySales(year, month);
        Map<String, Object> yearlyData = salesService.getYearlyChartData(year);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            addHeader(doc, "Monthly Sales Report", monthName + " " + year);
            addDivider(doc);
            addSummaryCards(doc, data);
            addDivider(doc);
            addTopProductsTable(doc, (List<Map<String, Object>>) data.get("topProducts"));
            addDivider(doc);
            addDailyBreakdownTable(doc, data, year, month);
            addDivider(doc);
            addYearlyComparisonTable(doc, yearlyData);
            addFooter(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate monthly PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    // ===== SECTIONS =====

    private void addHeader(Document doc, String title, String subtitle) {
        // Blue header background
        Table header = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(PRIMARY)
                .setBorder(null)
                .setMarginBottom(4);

        Cell cell = new Cell()
                .setBorder(null)
                .setPadding(20);

        cell.add(new Paragraph("ðŸ’§ Kreva")
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(11)
                .setMarginBottom(4));

        cell.add(new Paragraph(title)
                .setFontColor(ColorConstants.WHITE)
                .setFontSize(22)
                .setBold()
                .setMarginBottom(2));

        cell.add(new Paragraph(subtitle)
                .setFontColor(new DeviceRgb(191, 219, 254))
                .setFontSize(11));

        cell.add(new Paragraph("Generated: " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))
                .setFontColor(new DeviceRgb(147, 197, 253))
                .setFontSize(9)
                .setMarginTop(6));

        header.addCell(cell);
        doc.add(header);
    }

    private void addSummaryCards(Document doc, Map<String, Object> data) {
        doc.add(new Paragraph("Summary").setBold().setFontSize(13)
                .setFontColor(new DeviceRgb(30, 41, 59)).setMarginBottom(8));

        // Row 1: Total Orders + Revenue + Items
        Table row1 = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(8);

        row1.addCell(statCell("Total Orders",
                String.valueOf(data.get("totalOrders")), PRIMARY_LIGHT, PRIMARY));
        row1.addCell(statCell("Total Revenue",
                "Rs." + formatNum(data.get("totalRevenue")), SUCCESS_LIGHT, SUCCESS));
        row1.addCell(statCell("Items Sold",
                String.valueOf(data.get("totalItemsSold")),
                new DeviceRgb(237, 233, 254), new DeviceRgb(109, 40, 217)));

        doc.add(row1);

        // Row 2: Delivered + Pending + Cancelled
        Table row2 = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(8);

        row2.addCell(statCell("Delivered",
                String.valueOf(data.get("deliveredOrders")), SUCCESS_LIGHT, SUCCESS));
        row2.addCell(statCell("Pending",
                String.valueOf(data.get("pendingOrders")), WARNING_LIGHT,
                new DeviceRgb(133, 77, 14)));
        row2.addCell(statCell("Cancelled",
                String.valueOf(data.get("cancelledOrders")), DANGER_LIGHT, DANGER));

        doc.add(row2);
    }

    private Cell statCell(String label, String value, DeviceRgb bg, DeviceRgb fg) {
        Cell cell = new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(12)
                .setMargin(3);
        cell.add(new Paragraph(label).setFontSize(9).setFontColor(MUTED).setMarginBottom(4));
        cell.add(new Paragraph(value).setFontSize(18).setBold().setFontColor(fg));
        return cell;
    }

    private void addTopProductsTable(Document doc, List<Map<String, Object>> products) {
        doc.add(new Paragraph("Top Products").setBold().setFontSize(13)
                .setFontColor(new DeviceRgb(30, 41, 59)).setMarginBottom(8));

        if (products == null || products.isEmpty()) {
            doc.add(new Paragraph("No product data available.")
                    .setFontColor(MUTED).setFontSize(10).setMarginBottom(12));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3f, 1.5f, 2f}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableHeader(table, new String[]{"#", "Product Name", "Qty Sold", "Revenue"});

        int i = 1;
        for (Map<String, Object> p : products) {
            boolean alt = i % 2 == 0;
            table.addCell(bodyCell(String.valueOf(i++), alt));
            table.addCell(bodyCell(String.valueOf(p.get("name")), alt));
            table.addCell(bodyCell(String.valueOf(p.get("quantitySold")), alt));
            table.addCell(bodyCell("Rs." + formatNum(p.get("revenue")), alt));
        }

        doc.add(table);
        doc.add(new Paragraph("").setMarginBottom(8));
    }

    private void addOrdersTable(Document doc, List<Map<String, Object>> orders) {
        doc.add(new Paragraph("Orders Detail").setBold().setFontSize(13)
                .setFontColor(new DeviceRgb(30, 41, 59)).setMarginBottom(8));

        if (orders == null || orders.isEmpty()) {
            doc.add(new Paragraph("No orders found.")
                    .setFontColor(MUTED).setFontSize(10).setMarginBottom(12));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 2.5f, 1.5f, 1.5f, 2f}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableHeader(table, new String[]{"Order ID", "Customer", "Amount", "Status", "Time"});

        int i = 1;
        for (Map<String, Object> o : orders) {
            boolean alt = i % 2 == 0;
            table.addCell(bodyCell("#" + o.get("id"), alt));
            table.addCell(bodyCell(String.valueOf(o.get("customer")), alt));
            table.addCell(bodyCell("Rs." + formatNum(o.get("amount")), alt));

            String status = String.valueOf(o.get("status"));
            Cell statusCell = bodyCell(status, alt);
            colorizeStatus(statusCell, status);
            table.addCell(statusCell);

            String time = String.valueOf(o.get("time"));
            table.addCell(bodyCell(time.length() > 16 ? time.substring(11, 16) : time, alt));
            i++;
        }

        doc.add(table);
        doc.add(new Paragraph("").setMarginBottom(8));
    }

    private void addDailyBreakdownTable(Document doc, Map<String, Object> data, int year, int month) {
        doc.add(new Paragraph("Daily Breakdown").setBold().setFontSize(13)
                .setFontColor(new DeviceRgb(30, 41, 59)).setMarginBottom(8));

        Map<String, Object> dailyRevenue = (Map<String, Object>) data.get("dailyRevenue");
        Map<String, Object> dailyOrders  = (Map<String, Object>) data.get("dailyOrders");

        if (dailyRevenue == null) {
            doc.add(new Paragraph("No data.").setFontColor(MUTED).setFontSize(10));
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[]{1f, 2f, 2f}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableHeader(table, new String[]{"Day", "Orders", "Revenue"});

        int i = 1;
        for (Map.Entry<String, Object> entry : dailyRevenue.entrySet()) {
            boolean alt = i % 2 == 0;
            double rev = toDouble(entry.getValue());
            int orders = toInt(dailyOrders.get(entry.getKey()));

            table.addCell(bodyCell(entry.getKey(), alt));
            table.addCell(bodyCell(orders > 0 ? String.valueOf(orders) : "-", alt));
            table.addCell(bodyCell(rev > 0 ? "Rs." + formatNum(rev) : "-", alt));
            i++;
        }

        doc.add(table);
        doc.add(new Paragraph("").setMarginBottom(8));
    }

    private void addYearlyComparisonTable(Document doc, Map<String, Object> yearlyData) {
        if (yearlyData == null) return;

        doc.add(new Paragraph("Year Overview â€” " + yearlyData.get("year"))
                .setBold().setFontSize(13)
                .setFontColor(new DeviceRgb(30, 41, 59)).setMarginBottom(4));

        doc.add(new Paragraph("Annual Revenue: Rs." + formatNum(yearlyData.get("totalYearRevenue"))
                + "   |   Total Orders: " + yearlyData.get("totalYearOrders"))
                .setFontSize(10).setFontColor(MUTED).setMarginBottom(8));

        List<String> months = (List<String>) yearlyData.get("months");
        List<Number> revenue = (List<Number>) yearlyData.get("revenue");
        List<Number> counts  = (List<Number>) yearlyData.get("orderCounts");

        Table table = new Table(UnitValue.createPercentArray(new float[]{2f, 2f, 2f}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableHeader(table, new String[]{"Month", "Orders", "Revenue"});

        for (int i = 0; i < months.size(); i++) {
            boolean alt = i % 2 == 0;
            double rev = revenue.get(i).doubleValue();
            int cnt = counts.get(i).intValue();
            table.addCell(bodyCell(months.get(i), alt));
            table.addCell(bodyCell(cnt > 0 ? String.valueOf(cnt) : "-", alt));
            table.addCell(bodyCell(rev > 0 ? "Rs." + formatNum(rev) : "-", alt));
        }

        doc.add(table);
    }

    private void addFooter(Document doc) {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€")
                .setFontColor(BORDER).setFontSize(8).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Kreva â€” Kreva.in  |  Confidential Sales Report")
                .setFontSize(8).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER));
    }

    private void addDivider(Document doc) {
        doc.add(new Paragraph("").setMarginBottom(10));
    }

    // ===== TABLE HELPERS =====

    private void addTableHeader(Table table, String[] headers) {
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .setBackgroundColor(PRIMARY)
                    .setBorder(new SolidBorder(PRIMARY, 0.75f))
                    .setPadding(8)
                    .add(new Paragraph(h)
                            .setFontColor(ColorConstants.WHITE)
                            .setFontSize(9)
                            .setBold()));
        }
    }

    private Cell bodyCell(String text, boolean alt) {
        return new Cell()
                .setBackgroundColor(alt ? LIGHT_BG : ColorConstants.WHITE)
                .setBorder(new SolidBorder(BORDER, 0.75f))
                .setPadding(7)
                .add(new Paragraph(text == null ? "-" : text).setFontSize(9));
    }

    private void colorizeStatus(Cell cell, String status) {
        switch (status.toUpperCase()) {
            case "DELIVERED" -> cell.setBackgroundColor(SUCCESS_LIGHT)
                    .add(new Paragraph("").setFontColor(SUCCESS));
            case "CANCELLED" -> cell.setBackgroundColor(DANGER_LIGHT);
            case "PENDING"   -> cell.setBackgroundColor(WARNING_LIGHT);
        }
    }

    // ===== UTILS =====

    private String formatNum(Object val) {
        if (val == null) return "0.00";
        return String.format("%.2f", toDouble(val));
    }

    private double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); }
        catch (Exception e) { return 0.0; }
    }

    private int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); }
        catch (Exception e) { return 0; }
    }

    public byte[] exportOrderInvoicePdf(com.mineralwater.model.Order order) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            // Header: Brand & Title
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
            headerTable.addCell(new Cell().add(new Paragraph("Kreva")
                    .setFontSize(22).setBold().setFontColor(PRIMARY))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            headerTable.addCell(new Cell().add(new Paragraph("TAX INVOICE")
                    .setFontSize(22).setBold().setFontColor(MUTED).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            doc.add(headerTable);

            addDivider(doc);

            // Business info vs Invoice meta
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            Cell companyCell = new Cell().add(new Paragraph("From:")
                    .setBold().setFontColor(PRIMARY).setFontSize(11))
                    .add(new Paragraph("Kreva Office\nBelhe, Alephata, Junnar,\nDist: Pune - 412410, Maharashtra\nEmail: info@Kreva.in\nMobile: +91 8390252489")
                    .setFontSize(9))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            
            Cell metaCell = new Cell().add(new Paragraph()
                    .add(new Text("Invoice No: ").setBold())
                    .add(new Text("INV-" + order.getId() + "\n"))
                    .add(new Text("Date: ").setBold())
                    .add(new Text(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate().toString() : LocalDate.now().toString() + "\n"))
                    .add(new Text("Payment Method: ").setBold())
                    .add(new Text((order.getPaymentId() == null || order.getPaymentId().equals("COD") ? "Cash on Delivery" : (order.getPaymentId().startsWith("pi_") ? "Card / Stripe" : "UPI")) + "\n"))
                    .add(new Text("Payment Status: ").setBold())
                    .add(new Text(order.getPaymentStatus().toString() + "\n"))
                    .add(new Text(order.getPaymentId() != null && !order.getPaymentId().isBlank() ? "Txn ID: " + order.getPaymentId() : "")))
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            
            infoTable.addCell(companyCell);
            infoTable.addCell(metaCell);
            doc.add(infoTable);

            doc.add(new Paragraph("\n"));

            // Delivery Details card
            Table billTable = new Table(UnitValue.createPercentArray(new float[]{100})).useAllAvailableWidth();
            Cell billCell = new Cell().add(new Paragraph("Deliver To:")
                    .setBold().setFontColor(PRIMARY).setFontSize(11))
                    .add(new Paragraph()
                            .add(new Text("Customer: ").setBold())
                            .add(new Text(order.getUser().getName() + "\n"))
                            .add(new Text("Mobile: ").setBold())
                            .add(new Text(order.getUser().getMobileNumber() + "\n"))
                            .add(new Text("Delivery Address: ").setBold())
                            .add(new Text(order.getAddress())))
                    .setFontSize(9)
                    .setBackgroundColor(LIGHT_BG)
                    .setPadding(10)
                    .setBorder(new SolidBorder(BORDER, 1));
            billTable.addCell(billCell);
            doc.add(billTable);

            doc.add(new Paragraph("\n"));

            // Items Table
            Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20})).useAllAvailableWidth();
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Product Details").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(PRIMARY).setPadding(6));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Price").setBold().setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(PRIMARY).setPadding(6));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold().setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(PRIMARY).setPadding(6));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold().setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(PRIMARY).setPadding(6));

            for (com.mineralwater.model.OrderItem item : order.getItems()) {
                Paragraph pDetails = new Paragraph(item.getProduct().getName()).setFontSize(9);
                if (item.getBusinessName() != null && !item.getBusinessName().isBlank()) {
                    pDetails.add(new Text("\nCustomization: " + item.getBusinessName()).setFontSize(8).setFontColor(MUTED));
                }
                itemsTable.addCell(new Cell().add(pDetails).setPadding(6));
                itemsTable.addCell(new Cell().add(new Paragraph("â‚¹" + item.getPrice()).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(6));
                itemsTable.addCell(new Cell().add(new Paragraph(item.getQuantity().toString()).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(6));
                itemsTable.addCell(new Cell().add(new Paragraph("â‚¹" + (item.getPrice() * item.getQuantity())).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setPadding(6));
            }

            itemsTable.addCell(new Cell(1, 3).add(new Paragraph("Grand Total").setBold().setTextAlignment(TextAlignment.RIGHT)).setPadding(6).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            itemsTable.addCell(new Cell().add(new Paragraph("â‚¹" + order.getTotalAmount()).setBold().setFontColor(PRIMARY).setTextAlignment(TextAlignment.RIGHT)).setPadding(6));
            doc.add(itemsTable);

            addDivider(doc);

            Paragraph terms = new Paragraph("Thank you for your business!\nIf you have any questions about this invoice, please contact support@Kreva.in")
                    .setFontSize(8).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER).setItalic();
            doc.add(terms);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate order invoice PDF: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }
}
