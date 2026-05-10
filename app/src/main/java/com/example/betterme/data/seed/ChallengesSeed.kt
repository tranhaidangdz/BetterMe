package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity

/**
 * Static challenge catalog seeded into the [challenges] table on first launch.
 *
 * 50 curated challenges total — 10 per badge category — to back the Challenge Discovery
 * spec ("Each badge category should contain 10 predefined challenges"). IDs are explicit
 * (1..50) so [ChallengesSeed] can reference them via reward_badge_id from [BadgesSeed].
 *
 * Categories map to existing CategoryEntity rows seeded by `fakeCategories()`:
 *   1=Vận động, 2=Dinh dưỡng, 3=Tinh thần, 4=Học tập, 5=Kỷ luật, 6=Mối quan hệ.
 *
 * Difficulty distribution follows the user-facing tiers:
 *   EASY (7d) → MEDIUM (14d) → HARD (21–30d) → LEGENDARY (60–90d).
 *
 * Reward badges are bound by `reward_badge_id`; see [BadgesSeed] for the catalog
 * (1..20 across BASIC/HEALTH/LEARNING/SPECIAL plus 21..25 for DISCIPLINE).
 */
object ChallengesSeed {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun challenges(now: Long = System.currentTimeMillis()): List<ChallengeEntity> = listOf(

        // =====================================================================
        // 1. BASIC HABITS (10 challenges) — reward_badge_id 1..5
        // =====================================================================
        ChallengeEntity(
            id = 1, title = "Uống đủ 2L nước mỗi ngày",
            description = "Duy trì thói quen uống đủ nước mỗi ngày để cơ thể luôn khỏe mạnh và tràn đầy năng lượng.",
            short_description = "2 lít nước mỗi ngày", category_id = 1, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 1,
            icon_emoji = "💧", color_hex = "#3B82F6", is_featured = true,
            participant_count = 12500, sort_order = 1, created_at = now
        ),
        ChallengeEntity(
            id = 2, title = "Dậy sớm trước 7 giờ sáng",
            description = "Đặt nền móng cho một ngày năng suất bằng cách dậy đúng giờ.",
            short_description = "Bắt đầu ngày sớm", category_id = 5, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 2,
            icon_emoji = "🌅", color_hex = "#F59E0B",
            participant_count = 8420, sort_order = 2, created_at = now
        ),
        ChallengeEntity(
            id = 3, title = "Đánh răng đủ 2 lần/ngày",
            description = "Thói quen vệ sinh cơ bản — sáng và tối đều đặn.",
            short_description = "Răng miệng sạch sẽ", category_id = 5, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 60, reward_badge_id = 3,
            icon_emoji = "🪥", color_hex = "#06B6D4",
            participant_count = 5230, sort_order = 3, created_at = now
        ),
        ChallengeEntity(
            id = 4, title = "Ghi 3 điều biết ơn mỗi ngày",
            description = "Trau dồi tâm trạng tích cực bằng cách ghi nhận 3 điều bạn biết ơn mỗi tối.",
            short_description = "Mỗi tối 3 điều biết ơn", category_id = 3, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 3,
            icon_emoji = "🙏", color_hex = "#A855F7",
            participant_count = 3120, sort_order = 4, created_at = now
        ),
        ChallengeEntity(
            id = 5, title = "Đi bộ 5.000 bước/ngày",
            description = "Bắt đầu thói quen vận động nhẹ nhàng — mỗi ngày 5K bước.",
            short_description = "5K bước mỗi ngày", category_id = 1, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 70, reward_badge_id = 4,
            icon_emoji = "🚶", color_hex = "#10B981",
            participant_count = 4850, sort_order = 5, created_at = now
        ),
        ChallengeEntity(
            id = 6, title = "Hít thở sâu 5 phút",
            description = "Một bài tập thở đơn giản giúp giảm stress và tăng sự tập trung.",
            short_description = "5 phút thở sâu", category_id = 3, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 40, reward_badge_id = 5,
            icon_emoji = "🌬️", color_hex = "#0EA5E9",
            participant_count = 2010, sort_order = 6, created_at = now
        ),
        ChallengeEntity(
            id = 7, title = "Không bỏ bữa sáng 21 ngày",
            description = "Bữa sáng đầy đủ là nền tảng cho ngày làm việc hiệu quả.",
            short_description = "Bữa sáng mỗi ngày", category_id = 2, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 100, reward_badge_id = 4,
            icon_emoji = "🍳", color_hex = "#FB923C",
            participant_count = 1820, sort_order = 7, created_at = now
        ),
        ChallengeEntity(
            id = 8, title = "Ngủ đủ 7 tiếng",
            description = "Duy trì giấc ngủ chất lượng để cơ thể phục hồi và tinh thần luôn sảng khoái.",
            short_description = "7 tiếng ngủ chất lượng", category_id = 1, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 1,
            icon_emoji = "🌙", color_hex = "#6366F1",
            participant_count = 1800, sort_order = 8, created_at = now
        ),
        ChallengeEntity(
            id = 9, title = "Không sử dụng điện thoại 30 phút sau khi thức dậy",
            description = "Bắt đầu ngày tỉnh táo, không bị phân tâm bởi mạng xã hội.",
            short_description = "30p không điện thoại buổi sáng", category_id = 5, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 2,
            icon_emoji = "📵", color_hex = "#64748B",
            participant_count = 1100, sort_order = 9, created_at = now
        ),
        ChallengeEntity(
            id = 10, title = "Dọn giường mỗi sáng",
            description = "Một thói quen nhỏ tạo cảm giác hoàn thành ngay đầu ngày.",
            short_description = "Dọn giường buổi sáng", category_id = 5, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 60, reward_badge_id = 5,
            icon_emoji = "🛏️", color_hex = "#8B5CF6",
            participant_count = 940, sort_order = 10, created_at = now
        ),

        // =====================================================================
        // 2. HEALTH & FITNESS (10 challenges) — reward_badge_id 6..10
        // =====================================================================
        ChallengeEntity(
            id = 11, title = "Tập luyện 30 phút mỗi ngày",
            description = "Vận động cơ thể đều đặn — gym, chạy, hoặc tại nhà cũng được.",
            short_description = "30 phút vận động mỗi ngày", category_id = 1, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 120, reward_badge_id = 6,
            icon_emoji = "💪", color_hex = "#EF4444", is_featured = true,
            participant_count = 8700, sort_order = 11, created_at = now
        ),
        ChallengeEntity(
            id = 12, title = "Đi bộ 10.000 bước/ngày",
            description = "Vận động nhẹ nhàng đều đặn — 10K bước cho vóc dáng và tinh thần.",
            short_description = "10K bước mỗi ngày", category_id = 1, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 130, reward_badge_id = 10,
            icon_emoji = "🏃", color_hex = "#F97316",
            participant_count = 4100, sort_order = 12, created_at = now
        ),
        ChallengeEntity(
            id = 13, title = "Detox cơ thể 7 ngày",
            description = "Bắt đầu chế độ ăn lành mạnh trong 7 ngày để thanh lọc cơ thể.",
            short_description = "7 ngày ăn uống lành mạnh", category_id = 2, difficulty = "MEDIUM",
            duration_days = 7, target_streak = 7, reward_coins = 100, reward_badge_id = 9,
            icon_emoji = "🥗", color_hex = "#10B981", is_featured = true,
            participant_count = 1450, sort_order = 13, created_at = now
        ),
        ChallengeEntity(
            id = 14, title = "Không ăn đồ ngọt 14 ngày",
            description = "Cắt giảm đường để cải thiện sức khỏe tổng thể và năng lượng ổn định.",
            short_description = "Cắt giảm đường", category_id = 2, difficulty = "HARD",
            duration_days = 14, target_streak = 14, reward_coins = 200, reward_badge_id = 9,
            icon_emoji = "🍩", color_hex = "#EC4899", is_featured = true,
            participant_count = 2100, sort_order = 14, created_at = now
        ),
        ChallengeEntity(
            id = 15, title = "Yoga 15 phút mỗi sáng",
            description = "Khởi động cơ thể mỗi sáng bằng các động tác yoga nhẹ nhàng.",
            short_description = "Yoga buổi sáng", category_id = 1, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 90, reward_badge_id = 6,
            icon_emoji = "🧘", color_hex = "#A855F7",
            participant_count = 1320, sort_order = 15, created_at = now
        ),
        ChallengeEntity(
            id = 16, title = "Ngủ trước 11 giờ tối 21 ngày",
            description = "Đặt giấc ngủ ưu tiên — không thức khuya quá 23h.",
            short_description = "Đi ngủ sớm", category_id = 1, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 130, reward_badge_id = 8,
            icon_emoji = "😴", color_hex = "#3B82F6",
            participant_count = 980, sort_order = 16, created_at = now
        ),
        ChallengeEntity(
            id = 17, title = "Không ăn vặt sau 8 giờ tối",
            description = "Hạn chế ăn khuya để tiêu hóa và giấc ngủ tốt hơn.",
            short_description = "Không ăn sau 20h", category_id = 2, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 110, reward_badge_id = 9,
            icon_emoji = "🌃", color_hex = "#7C3AED",
            participant_count = 760, sort_order = 17, created_at = now
        ),
        ChallengeEntity(
            id = 18, title = "100 squats mỗi ngày",
            description = "Thử thách squat để tăng cường sức mạnh chân và mông.",
            short_description = "100 squats/ngày", category_id = 1, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 250, reward_badge_id = 10,
            icon_emoji = "🏋️", color_hex = "#DC2626",
            participant_count = 540, sort_order = 18, created_at = now
        ),
        ChallengeEntity(
            id = 19, title = "Ăn 5 phần rau củ/ngày",
            description = "Bổ sung đủ vitamin và chất xơ qua 5 phần rau củ mỗi ngày.",
            short_description = "5 phần rau củ/ngày", category_id = 2, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 140, reward_badge_id = 9,
            icon_emoji = "🥬", color_hex = "#22C55E",
            participant_count = 720, sort_order = 19, created_at = now
        ),
        ChallengeEntity(
            id = 20, title = "Không uống rượu/bia 30 ngày",
            description = "Cai bia rượu một tháng để cơ thể phục hồi.",
            short_description = "30 ngày dry month", category_id = 2, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 7,
            icon_emoji = "🚫", color_hex = "#0F172A",
            participant_count = 410, sort_order = 20, created_at = now
        ),

        // =====================================================================
        // 3. LEARNING & PRODUCTIVITY (10 challenges) — reward_badge_id 11..15
        // =====================================================================
        ChallengeEntity(
            id = 21, title = "Đọc sách 20 phút mỗi ngày",
            description = "Mở rộng kiến thức và rèn luyện sự tập trung qua việc đọc sách hàng ngày.",
            short_description = "20 phút đọc sách", category_id = 4, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 11,
            icon_emoji = "📚", color_hex = "#8B5CF6",
            participant_count = 3200, sort_order = 21, created_at = now
        ),
        ChallengeEntity(
            id = 22, title = "Học 10 từ vựng tiếng Anh/ngày",
            description = "Mở rộng vốn từ tiếng Anh đều đặn mỗi ngày.",
            short_description = "Mỗi ngày một chút từ vựng", category_id = 4, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 100, reward_badge_id = 12,
            icon_emoji = "🗣️", color_hex = "#06B6D4",
            participant_count = 2700, sort_order = 22, created_at = now
        ),
        ChallengeEntity(
            id = 23, title = "Viết nhật ký mỗi tối",
            description = "Ghi lại suy nghĩ và cảm xúc mỗi ngày để kết nối với bản thân.",
            short_description = "5 phút phản chiếu mỗi tối", category_id = 3, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 11,
            icon_emoji = "📝", color_hex = "#A855F7",
            participant_count = 950, sort_order = 23, created_at = now
        ),
        ChallengeEntity(
            id = 24, title = "Học 1 giờ kỹ năng mới/ngày",
            description = "Đầu tư 60 phút mỗi ngày cho một kỹ năng đang phát triển.",
            short_description = "1 giờ học mỗi ngày", category_id = 4, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 140, reward_badge_id = 12,
            icon_emoji = "🎒", color_hex = "#F59E0B",
            participant_count = 1180, sort_order = 24, created_at = now
        ),
        ChallengeEntity(
            id = 25, title = "Deep Work 2 giờ mỗi ngày",
            description = "Chìm sâu vào công việc — không điện thoại, không xao nhãng.",
            short_description = "2 giờ deep work", category_id = 4, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 220, reward_badge_id = 13,
            icon_emoji = "🎯", color_hex = "#0EA5E9",
            participant_count = 680, sort_order = 25, created_at = now
        ),
        ChallengeEntity(
            id = 26, title = "Học kỹ năng mới 30 ngày",
            description = "Dành thời gian học một kỹ năng mới mỗi ngày để phát triển bản thân.",
            short_description = "30 ngày kỹ năng mới", category_id = 4, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 350, reward_badge_id = 14,
            icon_emoji = "🎓", color_hex = "#0EA5E9",
            participant_count = 760, sort_order = 26, created_at = now
        ),
        ChallengeEntity(
            id = 27, title = "Hoàn thành 1 khóa học online 30 ngày",
            description = "Cam kết hoàn thành một khóa học online từ đầu đến cuối.",
            short_description = "Hoàn thành khóa học", category_id = 4, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 15,
            icon_emoji = "💻", color_hex = "#3B82F6",
            participant_count = 460, sort_order = 27, created_at = now
        ),
        ChallengeEntity(
            id = 28, title = "Đọc 1 chương sách mỗi ngày",
            description = "Duy trì văn hóa đọc — mỗi ngày một chương sách.",
            short_description = "Một chương mỗi ngày", category_id = 4, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 150, reward_badge_id = 11,
            icon_emoji = "📖", color_hex = "#7C3AED",
            participant_count = 1240, sort_order = 28, created_at = now
        ),
        ChallengeEntity(
            id = 29, title = "Nghe podcast học thuật 30 phút",
            description = "Tận dụng thời gian rảnh — nghe podcast khoa học, kinh doanh, tâm lý.",
            short_description = "30p podcast học thuật", category_id = 4, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 90, reward_badge_id = 12,
            icon_emoji = "🎧", color_hex = "#06B6D4",
            participant_count = 580, sort_order = 29, created_at = now
        ),
        ChallengeEntity(
            id = 30, title = "Viết blog/note hàng tuần",
            description = "Ghi chép những gì đã học — viết tổng kết tuần.",
            short_description = "1 bài viết mỗi tuần", category_id = 4, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 160, reward_badge_id = 13,
            icon_emoji = "✍️", color_hex = "#F59E0B",
            participant_count = 320, sort_order = 30, created_at = now
        ),

        // =====================================================================
        // 4. DISCIPLINE & CONSISTENCY (10 challenges) — reward_badge_id 21..25
        // =====================================================================
        ChallengeEntity(
            id = 31, title = "Không dùng điện thoại buổi sáng 7 ngày",
            description = "Bắt đầu ngày mới tỉnh táo, không bị phân tâm bởi mạng xã hội.",
            short_description = "Tránh xa điện thoại buổi sáng", category_id = 5, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 21,
            icon_emoji = "📵", color_hex = "#64748B",
            participant_count = 1100, sort_order = 31, created_at = now
        ),
        ChallengeEntity(
            id = 32, title = "30 ngày dậy sớm 6 giờ",
            description = "Đặt nền móng cho lịch sinh hoạt khoẻ mạnh và năng suất.",
            short_description = "Bắt đầu ngày sớm hơn", category_id = 5, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 300, reward_badge_id = 21,
            icon_emoji = "⏰", color_hex = "#FB923C",
            participant_count = 1200, sort_order = 32, created_at = now
        ),
        ChallengeEntity(
            id = 33, title = "Không mạng xã hội 1 tuần",
            description = "Detox kỹ thuật số để lấy lại sự tập trung và bình yên.",
            short_description = "1 tuần không scroll", category_id = 5, difficulty = "HARD",
            duration_days = 7, target_streak = 7, reward_coins = 150, reward_badge_id = 22,
            icon_emoji = "🚫", color_hex = "#0F172A",
            participant_count = 540, sort_order = 33, created_at = now
        ),
        ChallengeEntity(
            id = 34, title = "Nấu ăn ở nhà 14 ngày",
            description = "Hạn chế ăn ngoài, tự nấu để kiểm soát dinh dưỡng và tiết kiệm.",
            short_description = "Tự nấu mỗi ngày", category_id = 2, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 120, reward_badge_id = 21,
            icon_emoji = "🍳", color_hex = "#F59E0B",
            participant_count = 820, sort_order = 34, created_at = now
        ),
        ChallengeEntity(
            id = 35, title = "Không TikTok / Reels 14 ngày",
            description = "Cắt nguồn dopamine ngắn — lấy lại khả năng tập trung dài hạn.",
            short_description = "Không reels 14 ngày", category_id = 5, difficulty = "HARD",
            duration_days = 14, target_streak = 14, reward_coins = 180, reward_badge_id = 22,
            icon_emoji = "🎬", color_hex = "#DC2626",
            participant_count = 670, sort_order = 35, created_at = now
        ),
        ChallengeEntity(
            id = 36, title = "Không trễ hẹn 21 ngày",
            description = "Tôn trọng người khác và bản thân — đến đúng giờ mọi cuộc hẹn.",
            short_description = "Đúng giờ mọi nơi", category_id = 6, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 150, reward_badge_id = 21,
            icon_emoji = "⌚", color_hex = "#0EA5E9",
            participant_count = 290, sort_order = 36, created_at = now
        ),
        ChallengeEntity(
            id = 37, title = "Lên kế hoạch ngày mỗi sáng",
            description = "Bắt đầu ngày bằng 5 phút lên kế hoạch — top 3 priorities.",
            short_description = "5p kế hoạch buổi sáng", category_id = 5, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 110, reward_badge_id = 22,
            icon_emoji = "📋", color_hex = "#8B5CF6",
            participant_count = 480, sort_order = 37, created_at = now
        ),
        ChallengeEntity(
            id = 38, title = "Dọn dẹp 15 phút mỗi tối",
            description = "Giữ không gian gọn gàng — dọn dẹp 15 phút trước khi đi ngủ.",
            short_description = "15p dọn dẹp mỗi tối", category_id = 5, difficulty = "EASY",
            duration_days = 21, target_streak = 21, reward_coins = 130, reward_badge_id = 21,
            icon_emoji = "🧹", color_hex = "#10B981",
            participant_count = 380, sort_order = 38, created_at = now
        ),
        ChallengeEntity(
            id = 39, title = "Cai cà phê 14 ngày",
            description = "Thử thách bản thân không phụ thuộc caffeine.",
            short_description = "Không cà phê 14 ngày", category_id = 2, difficulty = "HARD",
            duration_days = 14, target_streak = 14, reward_coins = 170, reward_badge_id = 22,
            icon_emoji = "☕", color_hex = "#92400E",
            participant_count = 220, sort_order = 39, created_at = now
        ),
        ChallengeEntity(
            id = 40, title = "Không lướt mạng vô bổ 30 ngày",
            description = "30 ngày kỷ luật cao — chỉ truy cập internet khi có mục đích cụ thể.",
            short_description = "Internet có mục đích", category_id = 5, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 23,
            icon_emoji = "🛡️", color_hex = "#475569",
            participant_count = 180, sort_order = 40, created_at = now
        ),

        // =====================================================================
        // 5. SPECIAL & LEGENDARY (10 challenges) — reward_badge_id 16..20, 23..25
        // =====================================================================
        ChallengeEntity(
            id = 41, title = "Thiền 10 phút mỗi sáng",
            description = "Bắt đầu ngày mới với 10 phút thiền để giảm căng thẳng và tăng tập trung.",
            short_description = "10 phút tĩnh lặng mỗi sáng", category_id = 3, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 60, reward_badge_id = 16,
            icon_emoji = "🧘", color_hex = "#14B8A6",
            participant_count = 1800, sort_order = 41, created_at = now
        ),
        ChallengeEntity(
            id = 42, title = "[Sắp diễn ra] Chạy bộ tháng 6",
            description = "Thử thách chạy bộ 30 ngày — sắp diễn ra. Bật nhắc để không bỏ lỡ.",
            short_description = "30 ngày chạy bộ", category_id = 1, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 250, reward_badge_id = 16,
            icon_emoji = "🏃", color_hex = "#22C55E", is_featured = true,
            participant_count = 412, start_date = now + 14L * DAY_MS,
            sort_order = 42, created_at = now
        ),
        ChallengeEntity(
            id = 43, title = "[Nhóm] Cùng nhau khoẻ mạnh 14 ngày",
            description = "Thử thách nhóm — uống đủ nước và vận động cùng đồng đội.",
            short_description = "Thử thách nhóm 14 ngày", category_id = 1, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 150, reward_badge_id = 20,
            icon_emoji = "🤝", color_hex = "#1E40AF",
            is_group = true, is_featured = true,
            participant_count = 1247, sort_order = 43, created_at = now
        ),
        ChallengeEntity(
            id = 44, title = "[Nhóm] Đọc sách cùng nhau 7 ngày",
            description = "Thử thách nhóm — duy trì thói quen đọc sách cùng bạn bè.",
            short_description = "Đọc sách cùng nhóm", category_id = 4, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 80, reward_badge_id = 20,
            icon_emoji = "📖", color_hex = "#7C3AED", is_group = true,
            participant_count = 823, sort_order = 44, created_at = now
        ),
        ChallengeEntity(
            id = 45, title = "Dopamine Detox 21 ngày",
            description = "Cai mọi nguồn dopamine ngắn — không mạng xã hội, không nhạc nền, không snack. Một thử thách kỷ luật cao.",
            short_description = "21 ngày detox dopamine", category_id = 5, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 350, reward_badge_id = 22,
            icon_emoji = "🧠", color_hex = "#0EA5E9",
            participant_count = 290, sort_order = 45, created_at = now
        ),
        ChallengeEntity(
            id = 46, title = "Cold Shower 21 ngày",
            description = "Tắm nước lạnh mỗi sáng — tăng cường ý chí và sức khỏe.",
            short_description = "21 ngày tắm nước lạnh", category_id = 1, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 320, reward_badge_id = 17,
            icon_emoji = "🥶", color_hex = "#06B6D4",
            participant_count = 210, sort_order = 46, created_at = now
        ),
        ChallengeEntity(
            id = 47, title = "Wake Up at 5AM 60 ngày",
            description = "Dậy lúc 5h sáng trong 60 ngày liên tiếp — biến thói quen thành lối sống.",
            short_description = "60 ngày dậy 5h sáng", category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 600, reward_badge_id = 23,
            icon_emoji = "🌄", color_hex = "#7C3AED", is_featured = true,
            participant_count = 142, sort_order = 47, created_at = now
        ),
        ChallengeEntity(
            id = 48, title = "Monk Mode 60 ngày",
            description = "60 ngày kỷ luật cực đoan — chỉ tập trung vào sức khỏe, công việc, học tập. Không giải trí, không mạng xã hội.",
            short_description = "60 ngày kỷ luật cực đoan", category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 800, reward_badge_id = 23,
            icon_emoji = "🛕", color_hex = "#1E1B4B", is_featured = true,
            participant_count = 87, sort_order = 48, created_at = now
        ),
        ChallengeEntity(
            id = 49, title = "90-Day Transformation",
            description = "Cam kết 90 ngày toàn diện: tập luyện + ăn uống lành mạnh + ngủ đủ + học tập. Một phiên bản mới của bạn.",
            short_description = "90 ngày làm mới bản thân", category_id = 1, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 1000, reward_badge_id = 25,
            icon_emoji = "🔱", color_hex = "#EAB308", is_featured = true,
            participant_count = 64, sort_order = 49, created_at = now
        ),
        ChallengeEntity(
            id = 50, title = "Extreme Self-Improvement 90 ngày",
            description = "Phiên bản cuối cùng — 90 ngày chinh phục mọi cột mốc: sức khỏe, học tập, kỷ luật, tinh thần.",
            short_description = "90 ngày huyền thoại", category_id = 5, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 1200, reward_badge_id = 25,
            icon_emoji = "👑", color_hex = "#7C3AED", is_featured = true,
            participant_count = 38, sort_order = 50, created_at = now
        )
    )
}
