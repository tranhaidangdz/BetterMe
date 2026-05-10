package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity
import java.util.Calendar

/**
 * 20 seasonal "upcoming" challenges with start_date staggered across the next 12 months.
 *
 * Inserted alongside the main [ChallengesSeed] catalog by the [ChallengeSeederUseCase].
 * Each entry is `is_featured = true` and has a `start_date` in the future, so they appear
 * on:
 *   - the Overview screen's "Sắp diễn ra" tab (UpcomingChallengeRow with countdown bell)
 *   - the dedicated ChallengeUpcomingScreen (full list)
 *   - the new "Sắp diễn ra nổi bật" carousel on the Discovery screen
 *
 * IDs use the 100..119 range so they never collide with the main catalog (1..48) or any
 * future expansion.
 *
 * Dates are computed relative to the seed timestamp ("now") so a fresh install always
 * shows a healthy mix of upcoming events regardless of when the user signs up.
 */
object UpcomingChallengesSeed {

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun upcoming(now: Long = System.currentTimeMillis()): List<ChallengeEntity> {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        // Anchor each challenge to the 1st of an upcoming month so the carousel feels
        // like a real seasonal calendar.
        fun monthsAhead(months: Int, dayOfMonth: Int = 1): Long {
            val c = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, months)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return c.timeInMillis
        }

        return listOf(
            ChallengeEntity(
                id = 100, title = "January Reset Challenge",
                description = "Khởi đầu năm mới với một cuộc reset toàn diện. 21 ngày dọn dẹp thói quen cũ, xây dựng nền móng mới.",
                short_description = "21 ngày làm mới đầu năm",
                motivational_quote = "Năm mới không bắt đầu khi đồng hồ kêu — mà khi bạn quyết định.",
                completion_message = "Bạn đã đặt nền móng cho cả năm — phần còn lại chỉ là tiếp nối.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 220, reward_badge_id = 16,
                icon_emoji = "🎆", color_hex = "#3B82F6", is_featured = true,
                participant_count = 4820, start_date = monthsAhead(1, 1),
                sort_order = 100, created_at = now
            ),
            ChallengeEntity(
                id = 101, title = "New Year Discipline Sprint",
                description = "30 ngày kỷ luật cao độ ngay sau Tết — không quay lại với cám dỗ ngày lễ.",
                short_description = "30 ngày kỷ luật đầu năm",
                motivational_quote = "Lời hứa năm mới chỉ có giá trị khi giữ qua tuần thứ ba.",
                completion_message = "Bạn không chỉ vượt qua tháng 1 — bạn vừa bứt phá khỏi quán tính cũ.",
                category_id = 5, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 21,
                icon_emoji = "🚀", color_hex = "#7C3AED", is_featured = true,
                participant_count = 2640, start_date = monthsAhead(1, 8),
                sort_order = 101, created_at = now
            ),
            ChallengeEntity(
                id = 102, title = "February Self-Love Journey",
                description = "14 ngày dành cho chính mình: thiền, viết nhật ký, tập luyện, ngủ đủ.",
                short_description = "14 ngày yêu thương bản thân",
                motivational_quote = "Yêu bản thân không phải xa xỉ — đó là nền tảng.",
                completion_message = "Bạn vừa học cách trở thành người bạn tốt nhất của chính mình.",
                category_id = 3, difficulty = "EASY",
                duration_days = 14, target_streak = 14, reward_coins = 140, reward_badge_id = 27,
                icon_emoji = "💖", color_hex = "#EC4899", is_featured = true,
                participant_count = 3210, start_date = monthsAhead(2, 1),
                sort_order = 102, created_at = now
            ),
            ChallengeEntity(
                id = 103, title = "Spring Glow Up Challenge",
                description = "21 ngày làm đẹp toàn diện: skincare, nutrition, fitness, mindset. Chào xuân với phiên bản tốt hơn.",
                short_description = "21 ngày glow up mùa xuân",
                motivational_quote = "Mùa xuân là khi mọi thứ — kể cả bạn — được sinh ra lần nữa.",
                completion_message = "Bên trong bạn vừa nở hoa — và bên ngoài cũng vậy.",
                category_id = 1, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 200, reward_badge_id = 7,
                icon_emoji = "🌸", color_hex = "#F472B6", is_featured = true,
                participant_count = 5180, start_date = monthsAhead(3, 1),
                sort_order = 103, created_at = now
            ),
            ChallengeEntity(
                id = 104, title = "April Productivity Marathon",
                description = "30 ngày tối ưu năng suất: time-blocking, deep work, không xao nhãng. Quý 2 bắt đầu mạnh mẽ.",
                short_description = "30 ngày năng suất tối đa",
                motivational_quote = "Tháng 4 là khi mọi người rời ghế dự bị — bạn đang trên sân.",
                completion_message = "Bạn vừa hoàn thành tháng năng suất nhất trong năm.",
                category_id = 4, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 13,
                icon_emoji = "🎯", color_hex = "#0EA5E9", is_featured = true,
                participant_count = 1820, start_date = monthsAhead(4, 1),
                sort_order = 104, created_at = now
            ),
            ChallengeEntity(
                id = 105, title = "Summer Body Transformation",
                description = "60 ngày chuẩn bị cơ thể cho mùa hè: tập luyện cường độ cao, dinh dưỡng nghiêm ngặt, ngủ đủ.",
                short_description = "60 ngày summer body",
                motivational_quote = "Cơ thể mùa hè được xây dựng từ mùa xuân.",
                completion_message = "Bạn vừa hoàn thành 60 ngày khắc kỷ — và mùa hè đang chờ.",
                category_id = 1, difficulty = "LEGENDARY",
                duration_days = 60, target_streak = 60, reward_coins = 700, reward_badge_id = 7,
                icon_emoji = "🏖️", color_hex = "#F97316", is_featured = true,
                participant_count = 4640, start_date = monthsAhead(5, 1),
                sort_order = 105, created_at = now
            ),
            ChallengeEntity(
                id = 106, title = "June Consistency Camp",
                description = "21 ngày tập trung vào một thói quen duy nhất — không bỏ một ngày. Khám phá sức mạnh của tính nhất quán.",
                short_description = "21 ngày kiên định tuyệt đối",
                motivational_quote = "Nhất quán đánh bại tài năng khi tài năng không nhất quán.",
                completion_message = "Bạn vừa khám phá vũ khí thực sự: kiên định mỗi ngày.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 220, reward_badge_id = 21,
                icon_emoji = "⛺", color_hex = "#10B981", is_featured = true,
                participant_count = 1980, start_date = monthsAhead(6, 1),
                sort_order = 106, created_at = now
            ),
            ChallengeEntity(
                id = 107, title = "Morning Warrior July",
                description = "30 ngày dậy 5h sáng + tập luyện ngay lập tức. Mùa hè là lúc các chiến binh tỉnh dậy.",
                short_description = "30 ngày Morning Warrior",
                motivational_quote = "Mặt trời mọc cho mọi người — chỉ một số ít có mặt để đón nó.",
                completion_message = "30 buổi bình minh huy hoàng — bạn đã sống chúng đầy đủ.",
                category_id = 1, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 360, reward_badge_id = 7,
                icon_emoji = "🌄", color_hex = "#FBBF24", is_featured = true,
                participant_count = 1240, start_date = monthsAhead(7, 1),
                sort_order = 107, created_at = now
            ),
            ChallengeEntity(
                id = 108, title = "August Deep Focus Month",
                description = "30 ngày deep work 2 giờ mỗi ngày, không xao nhãng. Cuối hè là lúc bứt phá năng suất.",
                short_description = "30 ngày deep work cao độ",
                motivational_quote = "Sự tập trung sâu là đặc quyền của người không bị mạng xã hội cướp.",
                completion_message = "Bạn vừa rèn luyện não như rèn cơ bắp — và nó mạnh hơn.",
                category_id = 4, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 340, reward_badge_id = 13,
                icon_emoji = "🧠", color_hex = "#0EA5E9", is_featured = true,
                participant_count = 980, start_date = monthsAhead(8, 1),
                sort_order = 108, created_at = now
            ),
            ChallengeEntity(
                id = 109, title = "September Study Grind",
                description = "30 ngày học 2 giờ mỗi ngày — quay lại trường lớp, dù bạn đi học hay không.",
                short_description = "30 ngày study grind",
                motivational_quote = "Tháng 9 là tháng của những người chọn học — không phải chọn lười.",
                completion_message = "Bạn vừa thắng cuộc đua thầm lặng nhất: kiên trì học mỗi ngày.",
                category_id = 4, difficulty = "MEDIUM",
                duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 14,
                icon_emoji = "📚", color_hex = "#8B5CF6", is_featured = true,
                participant_count = 2780, start_date = monthsAhead(9, 1),
                sort_order = 109, created_at = now
            ),
            ChallengeEntity(
                id = 110, title = "October Dopamine Detox",
                description = "21 ngày cai dopamine ngắn — không Reels, không snack, không nội dung gây nghiện. Lấy lại bộ não.",
                short_description = "21 ngày detox dopamine",
                motivational_quote = "Não bạn xứng đáng với phần thưởng tốt hơn những clip 30 giây.",
                completion_message = "Bạn vừa lấy lại quyền điều khiển hệ thống thưởng của não bộ.",
                category_id = 5, difficulty = "HARD",
                duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 22,
                icon_emoji = "🧘‍♂️", color_hex = "#475569", is_featured = true,
                participant_count = 1620, start_date = monthsAhead(10, 1),
                sort_order = 110, created_at = now
            ),
            ChallengeEntity(
                id = 111, title = "No Sugar November",
                description = "30 ngày không đường tinh chế. Reset vị giác trước mùa lễ hội cuối năm.",
                short_description = "30 ngày không đường",
                motivational_quote = "Đường ngọt — nhưng kỷ luật ngọt hơn nhiều.",
                completion_message = "Vị giác đã reset, năng lượng ổn định, và bạn vừa sẵn sàng cho tháng 12.",
                category_id = 2, difficulty = "HARD",
                duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 9,
                icon_emoji = "🍬", color_hex = "#EC4899", is_featured = true,
                participant_count = 2480, start_date = monthsAhead(11, 1),
                sort_order = 111, created_at = now
            ),
            ChallengeEntity(
                id = 112, title = "December Reflection Challenge",
                description = "21 ngày phản chiếu cuối năm: viết nhật ký, lập kế hoạch, biết ơn. Khép lại năm cũ một cách trọn vẹn.",
                short_description = "21 ngày phản chiếu cuối năm",
                motivational_quote = "Năm tốt là năm bạn đã thực sự sống — không phải chỉ trải qua.",
                completion_message = "Bạn vừa khép lại năm cũ một cách trọn vẹn nhất.",
                category_id = 3, difficulty = "EASY",
                duration_days = 21, target_streak = 21, reward_coins = 200, reward_badge_id = 27,
                icon_emoji = "📓", color_hex = "#A855F7", is_featured = true,
                participant_count = 3120, start_date = monthsAhead(12, 1),
                sort_order = 112, created_at = now
            ),
            ChallengeEntity(
                id = 113, title = "Winter Monk Mode",
                description = "60 ngày Monk Mode mùa đông — kỷ luật cực đoan khi cả thế giới chậm lại.",
                short_description = "60 ngày Monk Mode mùa đông",
                motivational_quote = "Mùa đông là mùa của những người dành công việc cho mình.",
                completion_message = "60 ngày tĩnh lặng — bạn vừa rèn được phiên bản mạnh nhất của mình.",
                category_id = 5, difficulty = "LEGENDARY",
                duration_days = 60, target_streak = 60, reward_coins = 800, reward_badge_id = 18,
                icon_emoji = "❄️", color_hex = "#1E1B4B", is_featured = true,
                participant_count = 540, start_date = monthsAhead(0, 15),
                sort_order = 113, created_at = now
            ),
            ChallengeEntity(
                id = 114, title = "Weekend Warrior Challenge",
                description = "Mỗi cuối tuần trong 4 tuần: chỉ làm việc có ý nghĩa, không nướng cuối tuần lên nội dung rác.",
                short_description = "4 cuối tuần có chất lượng",
                motivational_quote = "Cách bạn dành cuối tuần là cách bạn dành cuộc đời.",
                completion_message = "Bạn vừa lấy lại 8 ngày mỗi tháng cho mục tiêu thật.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 28, target_streak = 28, reward_coins = 220, reward_badge_id = 21,
                icon_emoji = "🗓️", color_hex = "#F97316", is_featured = true,
                participant_count = 1480, start_date = monthsAhead(0, 8),
                sort_order = 114, created_at = now
            ),
            ChallengeEntity(
                id = 115, title = "7-Day Quick Reset",
                description = "Một tuần ngắn nhưng đủ để khởi động lại. Dành cho ai cần cú hích nhanh.",
                short_description = "7 ngày reset nhanh",
                motivational_quote = "Đôi khi tất cả những gì bạn cần là 7 ngày trung thực với mình.",
                completion_message = "Một tuần — đủ để đặt nền móng cho cả tháng.",
                category_id = 5, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 80, reward_badge_id = 21,
                icon_emoji = "⚡", color_hex = "#FBBF24", is_featured = true,
                participant_count = 5840, start_date = monthsAhead(0, 5),
                sort_order = 115, created_at = now
            ),
            ChallengeEntity(
                id = 116, title = "Mental Wellness Week",
                description = "7 ngày tập trung vào sức khỏe tinh thần: thiền, biết ơn, sống chậm, nói chuyện với người thân.",
                short_description = "Tuần lễ sức khỏe tinh thần",
                motivational_quote = "Tâm trí khỏe là nền tảng của mọi thành công khác.",
                completion_message = "Tâm hồn của bạn vừa được chăm sóc 7 ngày liên tục — và nó đáp lại.",
                category_id = 3, difficulty = "EASY",
                duration_days = 7, target_streak = 7, reward_coins = 90, reward_badge_id = 28,
                icon_emoji = "🧘", color_hex = "#14B8A6", is_featured = true,
                participant_count = 3240, start_date = monthsAhead(0, 12),
                sort_order = 116, created_at = now
            ),
            ChallengeEntity(
                id = 117, title = "Better Sleep Challenge",
                description = "21 ngày tối ưu giấc ngủ: ngủ trước 11h, không màn hình 1 giờ trước ngủ, phòng ngủ tối + mát.",
                short_description = "21 ngày ngủ tốt hơn",
                motivational_quote = "Giấc ngủ tốt không phải xa xỉ — đó là nền tảng.",
                completion_message = "Bạn vừa khôi phục được tài sản đáng giá nhất: giấc ngủ.",
                category_id = 1, difficulty = "MEDIUM",
                duration_days = 21, target_streak = 21, reward_coins = 200, reward_badge_id = 8,
                icon_emoji = "😴", color_hex = "#6366F1", is_featured = true,
                participant_count = 2840, start_date = monthsAhead(0, 22),
                sort_order = 117, created_at = now
            ),
            ChallengeEntity(
                id = 118, title = "New Month New Me",
                description = "Bắt đầu tháng mới với 14 ngày làm mới một mảng đời sống — bất kỳ mảng nào bạn chọn.",
                short_description = "14 ngày làm mới đầu tháng",
                motivational_quote = "Mỗi tháng là một bản nháp mới của con người bạn muốn trở thành.",
                completion_message = "Bạn vừa chứng minh: mỗi 30 ngày là một cơ hội tái sinh.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 14, target_streak = 14, reward_coins = 160, reward_badge_id = 21,
                icon_emoji = "🌟", color_hex = "#A855F7", is_featured = true,
                participant_count = 4320, start_date = monthsAhead(1, 1),
                sort_order = 118, created_at = now
            ),
            ChallengeEntity(
                id = 119, title = "30-Day Comeback Challenge",
                description = "30 ngày dành cho ai đã mất nhịp. Không quan trọng bạn đã rơi xa — chỉ quan trọng bạn đứng dậy.",
                short_description = "30 ngày trở lại đường đua",
                motivational_quote = "Người mạnh nhất không phải người không bao giờ rơi — mà là người đứng dậy mỗi lần.",
                completion_message = "Bạn vừa chứng minh điều quý giá nhất: bạn không bao giờ thực sự gục ngã.",
                category_id = 5, difficulty = "MEDIUM",
                duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 23,
                icon_emoji = "💪", color_hex = "#DC2626", is_featured = true,
                participant_count = 6240, start_date = monthsAhead(0, 28),
                sort_order = 119, created_at = now
            )
        )
    }
}
