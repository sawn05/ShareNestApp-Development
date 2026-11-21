
package com.example.sharecare.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.List;

public class SharenestProduct {

    public String id;
    public String name;
    public String category;
    public String address;
    public String description;
    public String coverImageUrl;
    public String status;

    public double latitude;
    public double longitude;
    public double distanceKm = -1;

    public long quantity;
    public long quantityAvailable;
    public String getId() {
        return id;
    }
    public Date createdAt;

    public static SharenestProduct from(DocumentSnapshot doc) {

        SharenestProduct p = new SharenestProduct();

        p.id = doc.getId();

        p.name = text(doc, "name");
        p.category = text(doc, "category");
        p.address = text(doc, "address");
        p.description = text(doc, "description");
        p.coverImageUrl = text(doc, "coverImageUrl");
        p.status = text(doc, "status");

        // Láº¥y thá»i gian Ä‘Äƒng bÃ i
        Timestamp timestamp = doc.getTimestamp("createdAt");

        if (timestamp != null) {
            p.createdAt = timestamp.toDate();
        }

        // KhÃ´ng hiá»ƒn thá»‹ sáº£n pháº©m Ä‘Ã£ xÃ³a
        if (Boolean.TRUE.equals(doc.getBoolean("isDeleted"))) {
            return null;
        }

        // KhÃ´ng hiá»ƒn thá»‹ sáº£n pháº©m bá»‹ áº©n
        String moderation = text(doc, "moderationStatus");

        if (!moderation.isEmpty()
                && !"VISIBLE".equals(moderation)) {
            return null;
        }

        // Hiá»ƒn thá»‹ AVAILABLE vÃ  RESERVED
        // KhÃ´ng hiá»ƒn thá»‹ COMPLETED
        if ("COMPLETED".equals(p.status)) {
            return null;
        }

        // Äá»c tá»•ng sá»‘ lÆ°á»£ng
        Number total = (Number) doc.get("quantity");
        p.quantity = total == null ? 0 : total.longValue();

        // Æ¯u tiÃªn quantityAvailable náº¿u cÃ³
        Number available =
                (Number) doc.get("quantityAvailable");

        if (available != null) {
            p.quantityAvailable = available.longValue();
        } else {
            // TÆ°Æ¡ng thÃ­ch dá»¯ liá»‡u cÅ©
            p.quantityAvailable =
                    "AVAILABLE".equals(p.status)
                            ? p.quantity : 0;
        }

        // Äá»c tá»a Ä‘á»™ náº¿u cÃ³
        Number lat = (Number) doc.get("latitude");
        Number lon = (Number) doc.get("longitude");

        p.latitude = lat == null ? 0 : lat.doubleValue();
        p.longitude = lon == null ? 0 : lon.doubleValue();

        // Láº¥y áº£nh Ä‘áº§u tiÃªn náº¿u chÆ°a cÃ³ áº£nh Ä‘áº¡i diá»‡n
        if (p.coverImageUrl.isEmpty()) {

            Object images = doc.get("imageUrls");

            if (images instanceof List) {

                List<?> imageList = (List<?>) images;

                if (!imageList.isEmpty()
                        && imageList.get(0) instanceof String) {

                    p.coverImageUrl =
                            (String) imageList.get(0);
                }
            }
        }

        return p;
    }

    private static String text(
            DocumentSnapshot doc,
            String field
    ) {
        String value = doc.getString(field);
        return value == null ? "" : value;
    }
}
