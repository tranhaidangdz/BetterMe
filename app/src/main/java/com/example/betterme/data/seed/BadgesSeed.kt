package com.example.betterme.data.seed

import com.example.betterme.R
import com.example.betterme.data.local.room.entities.AchievementEntity

/**
 * Static badge catalog seeded into the [achievements] table on first launch.
 *
 * IDs are explicit (1..20) so [ChallengesSeed] can reference them via reward_badge_id.
 * Layout mirrors the design mockup:
 *
 * - 1..5  BASIC     "Huy hiệu cơ bản"
 * - 6..10 HEALTH    "Huy hiệu sức khỏe"
 * - 11..15 LEARNING "Huy hiệu học tập"
 * - 16..20 SPECIAL  "Huy hiệu đặc biệt"
 *
 * The `icon` field on each row is the drawable resource id of the production PNG art.
 * Emoji + color_hex are kept as fallbacks in case a drawable is ever missing.
 */
object BadgesSeed {

    val badges: List<AchievementEntity> = listOf(
        // ==== 1. Huy hiệu cơ bản (1-5) ====
        AchievementEntity(
            id = 1,
            title = "Giọt Nước",
            description = "Uống nước",
            icon = R.drawable.ic_coban_giotnuoc,
            category = "BASIC",
            icon_emoji = "💧",
            color_hex = "#3B82F6",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 1,
            sort_order = 1
        ),
        AchievementEntity(
            id = 2,
            title = "Kiên Trì",
            description = "Liên tiếp 7 ngày",
            icon = R.drawable.ic_coban_kientri,
            category = "BASIC",
            icon_emoji = "🔥",
            color_hex = "#F59E0B",
            criteria_type = "STREAK",
            criteria_value = 7,
            sort_order = 2
        ),
        AchievementEntity(
            id = 3,
            title = "Bước Khởi Đầu",
            description = "Hoàn thành đầu tiên",
            icon = R.drawable.ic_coban_buockhoidau,
            category = "BASIC",
            icon_emoji = "⭐",
            color_hex = "#FBBF24",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            sort_order = 3
        ),
        AchievementEntity(
            id = 4,
            title = "Siêu Tốc",
            description = "Hoàn thành 3 lần",
            icon = R.drawable.ic_coban_sieutoc,
            category = "BASIC",
            icon_emoji = "⚡",
            color_hex = "#EF4444",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 3,
            sort_order = 4
        ),
        AchievementEntity(
            id = 5,
            title = "Ngôi Sao",
            description = "Hoàn thành 10 lần",
            icon = R.drawable.ic_coban_ngoisao,
            category = "BASIC",
            icon_emoji = "🌟",
            color_hex = "#FBBF24",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 10,
            sort_order = 5
        ),

        // ==== 2. Huy hiệu sức khỏe (6-10) ====
        AchievementEntity(
            id = 6,
            title = "Năng Lượng",
            description = "Tập luyện",
            icon = R.drawable.ic_suckhoe_nangluong,
            category = "HEALTH",
            icon_emoji = "💪",
            color_hex = "#10B981",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 2,
            sort_order = 6
        ),
        AchievementEntity(
            id = 7,
            title = "Sống Khỏe",
            description = "30 ngày",
            icon = R.drawable.ic_suckhoe_songkhoe,
            category = "HEALTH",
            icon_emoji = "🐦",
            color_hex = "#F97316",
            criteria_type = "STREAK",
            criteria_value = 30,
            sort_order = 7
        ),
        AchievementEntity(
            id = 8,
            title = "Ngủ Ngon",
            description = "Ngủ đủ giấc",
            icon = R.drawable.ic_suckhoe_ngungon,
            category = "HEALTH",
            icon_emoji = "🌙",
            color_hex = "#6366F1",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 4,
            sort_order = 8
        ),
        AchievementEntity(
            id = 9,
            title = "Ăn Uống Lành Mạnh",
            description = "Ăn uống tốt",
            icon = R.drawable.ic_suckhoe_anuonglanhmanh,
            category = "HEALTH",
            icon_emoji = "🍎",
            color_hex = "#10B981",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 7,
            sort_order = 9
        ),
        AchievementEntity(
            id = 10,
            title = "Thân Hình Đẹp",
            description = "Duy trì tập luyện",
            icon = R.drawable.ic_suckhoe_thanhinhdep,
            category = "HEALTH",
            icon_emoji = "🏃",
            color_hex = "#8B5CF6",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 8,
            sort_order = 10
        ),

        // ==== 3. Huy hiệu học tập (11-15) ====
        AchievementEntity(
            id = 11,
            title = "Đọc Sách",
            description = "10 ngày",
            icon = R.drawable.ic_hoctap_docsach,
            category = "LEARNING",
            icon_emoji = "📚",
            color_hex = "#8B5CF6",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 3,
            sort_order = 11
        ),
        AchievementEntity(
            id = 12,
            title = "Học Tập",
            description = "Chăm chỉ",
            icon = R.drawable.ic_hoctap_hoctap,
            category = "LEARNING",
            icon_emoji = "🎒",
            color_hex = "#F59E0B",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 10,
            sort_order = 12
        ),
        AchievementEntity(
            id = 13,
            title = "Tập Trung",
            description = "Không xao nhãng",
            icon = R.drawable.ic_hoctap_taptrung,
            category = "LEARNING",
            icon_emoji = "🧠",
            color_hex = "#06B6D4",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 9,
            sort_order = 13
        ),
        AchievementEntity(
            id = 14,
            title = "Kiến Thức",
            description = "Không ngừng học",
            icon = R.drawable.ic_hoctap_kienthuc,
            category = "LEARNING",
            icon_emoji = "🎓",
            color_hex = "#0EA5E9",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 1,
            criteria_challenge_id = 15,
            sort_order = 14
        ),
        AchievementEntity(
            id = 15,
            title = "Thủ Khoa",
            description = "Xuất sắc",
            icon = R.drawable.ic_hoctap_thukhoa,
            category = "LEARNING",
            icon_emoji = "🏆",
            color_hex = "#EAB308",
            criteria_type = "TOTAL_CHECKINS",
            criteria_value = 100,
            sort_order = 15
        ),

        // ==== 4. Huy hiệu đặc biệt (16-20) ====
        AchievementEntity(
            id = 16,
            title = "Thử Thách 30 Ngày",
            description = "Hoàn thành",
            icon = R.drawable.ic_dacbiet_thuthach30ngay,
            category = "SPECIAL",
            icon_emoji = "🔥",
            color_hex = "#DC2626",
            criteria_type = "STREAK",
            criteria_value = 30,
            sort_order = 16
        ),
        AchievementEntity(
            id = 17,
            title = "Bậc Thầy",
            description = "10 thử thách",
            icon = R.drawable.ic_dacbiet_bacthay,
            category = "SPECIAL",
            icon_emoji = "🏆",
            color_hex = "#9333EA",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 10,
            sort_order = 17
        ),
        AchievementEntity(
            id = 18,
            title = "Huyền Thoại",
            description = "20 thử thách",
            icon = R.drawable.ic_dacbiet_huyenthoai,
            category = "SPECIAL",
            icon_emoji = "⭐",
            color_hex = "#475569",
            criteria_type = "CHALLENGE_COMPLETED",
            criteria_value = 20,
            sort_order = 18
        ),
        AchievementEntity(
            id = 19,
            title = "Vô Địch",
            description = "Top 1",
            icon = R.drawable.ic_dacbiet_vodich,
            category = "SPECIAL",
            icon_emoji = "👑",
            color_hex = "#EAB308",
            criteria_type = "MANUAL",
            criteria_value = 0,
            sort_order = 19
        ),
        AchievementEntity(
            id = 20,
            title = "Người Truyền Cảm Hứng",
            description = "Giúp đỡ người khác",
            icon = R.drawable.ic_dacbiet_nguoitruyencamhung,
            category = "SPECIAL",
            icon_emoji = "✨",
            color_hex = "#F97316",
            criteria_type = "MANUAL",
            criteria_value = 0,
            sort_order = 20
        )
    )
}
