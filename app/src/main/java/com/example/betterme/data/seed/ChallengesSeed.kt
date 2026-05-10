package com.example.betterme.data.seed

import com.example.betterme.data.local.room.entities.ChallengeEntity

/**
 * Curated challenge catalog seeded into the [challenges] table on first launch.
 *
 * 48 challenges total — 8 per badge category — backing the production-grade Challenge
 * Discovery spec ("around 40-50 fully predefined challenges, evenly distributed across
 * 6 categories: Basic Habits, Health & Fitness, Learning & Productivity, Mental Wellness,
 * Discipline & Consistency, Special / Legendary"). Each entry includes a unique
 * motivational quote and completion message so the UI never falls back to generic copy.
 *
 * IDs are explicit (1..48) so [BadgesSeed] reward bindings stay deterministic.
 *
 * CategoryEntity mapping (`category_id`):
 *   1 = Vận động & thể chất, 2 = Dinh dưỡng, 3 = Tinh thần,
 *   4 = Học tập, 5 = Kỷ luật, 6 = Mối quan hệ.
 *
 * Difficulty tiers map 1:1 with the user-facing
 * [com.example.betterme.presentation.challenge.shared.Difficulty]:
 *   EASY / MEDIUM / HARD / LEGENDARY.
 */
object ChallengesSeed {

    fun challenges(now: Long = System.currentTimeMillis()): List<ChallengeEntity> = listOf(

        // =====================================================================
        // 1. BASIC HABITS (8) — reward_badge_id 1..5
        // =====================================================================
        ChallengeEntity(
            id = 1, title = "Uống đủ nước mỗi ngày",
            description = "Cơ thể bạn 60% là nước. Một thử thách 7 ngày đơn giản nhưng hiệu quả: uống đủ 2 lít nước mỗi ngày để khởi động lại sức khỏe.",
            short_description = "2 lít nước/ngày trong 7 ngày",
            motivational_quote = "Mỗi ngụm nước là một lần chăm sóc bản thân.",
            completion_message = "Bạn đã xây xong nền móng — cơ thể đầy nước, đầy năng lượng.",
            category_id = 1, difficulty = "EASY",
            duration_days = 7, target_streak = 7, reward_coins = 50, reward_badge_id = 1,
            icon_emoji = "💧", color_hex = "#3B82F6", is_featured = true,
            participant_count = 12500, sort_order = 1, created_at = now
        ),
        ChallengeEntity(
            id = 2, title = "Dậy trước 7 giờ sáng",
            description = "14 ngày dậy đúng giờ — đặt nền móng cho lịch sinh hoạt năng suất. Buổi sáng sớm là tài sản bạn đang bỏ lỡ.",
            short_description = "Bắt đầu ngày trước 7AM",
            motivational_quote = "Người chiến thắng buổi sáng là người chiến thắng cả ngày.",
            completion_message = "Đồng hồ sinh học của bạn đã được lập trình lại — buổi sáng giờ là của bạn.",
            category_id = 5, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 80, reward_badge_id = 2,
            icon_emoji = "🌅", color_hex = "#F59E0B",
            participant_count = 8420, sort_order = 2, created_at = now
        ),
        ChallengeEntity(
            id = 3, title = "Dọn giường mỗi sáng",
            description = "Một thói quen nhỏ nhưng tạo cảm giác hoàn thành ngay đầu ngày. 21 ngày để biến nó thành phản xạ.",
            short_description = "Dọn giường 21 ngày liên tiếp",
            motivational_quote = "Muốn thay đổi thế giới, bắt đầu bằng việc dọn giường — Đô đốc McRaven.",
            completion_message = "Mỗi sáng giờ bạn bắt đầu bằng một chiến thắng nhỏ.",
            category_id = 5, difficulty = "EASY",
            duration_days = 21, target_streak = 21, reward_coins = 120, reward_badge_id = 3,
            icon_emoji = "🛏️", color_hex = "#8B5CF6",
            participant_count = 5230, sort_order = 3, created_at = now
        ),
        ChallengeEntity(
            id = 4, title = "Đi bộ buổi sáng 14 ngày",
            description = "20 phút đi bộ ngoài trời mỗi sáng — không gian, ánh sáng, và bước chân. Một liều thuốc tự nhiên cho tâm trí và cơ thể.",
            short_description = "20 phút đi bộ mỗi sáng",
            motivational_quote = "Đôi chân biết câu trả lời mà tâm trí chưa tìm ra.",
            completion_message = "Bạn đã tìm thấy nhịp điệu của riêng mình.",
            category_id = 1, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 90, reward_badge_id = 4,
            icon_emoji = "🚶", color_hex = "#10B981",
            participant_count = 4850, sort_order = 4, created_at = now
        ),
        ChallengeEntity(
            id = 5, title = "Không bỏ bữa sáng 21 ngày",
            description = "Bữa sáng đầy đủ là nền tảng cho ngày làm việc hiệu quả. 21 ngày để hình thành thói quen tốt.",
            short_description = "Bữa sáng mỗi ngày",
            motivational_quote = "Bữa sáng là lá thư tình đầu tiên bạn gửi cho cơ thể mỗi ngày.",
            completion_message = "Cơ thể bạn cảm ơn vì 21 buổi sáng được nuôi dưỡng đúng cách.",
            category_id = 2, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 140, reward_badge_id = 4,
            icon_emoji = "🍳", color_hex = "#FB923C",
            participant_count = 1820, sort_order = 5, created_at = now
        ),
        ChallengeEntity(
            id = 6, title = "8 ly nước mỗi ngày — 30 ngày",
            description = "Mục tiêu trung hạn: duy trì 8 ly nước mỗi ngày trong 30 ngày liên tiếp. Mức độ hydrat hóa cao hơn cho hiệu suất tốt hơn.",
            short_description = "8 ly nước trong 30 ngày",
            motivational_quote = "Nước không phải là một lựa chọn, đó là nền tảng.",
            completion_message = "Cơ thể bạn đã quen với mức năng lượng mới — không quay lại nữa.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 180, reward_badge_id = 1,
            icon_emoji = "🥤", color_hex = "#06B6D4",
            participant_count = 2340, sort_order = 6, created_at = now
        ),
        ChallengeEntity(
            id = 7, title = "Giãn cơ mỗi ngày 14 ngày",
            description = "10 phút giãn cơ mỗi ngày để giải tỏa căng thẳng cơ bắp và phòng tránh đau lưng, đau cổ.",
            short_description = "10 phút giãn cơ/ngày",
            motivational_quote = "Cơ thể dẻo dai là tâm trí dẻo dai.",
            completion_message = "Bạn đã trao cho cơ thể một món quà mỗi ngày — và nó đáp lại.",
            category_id = 1, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 80, reward_badge_id = 5,
            icon_emoji = "🤸", color_hex = "#22C55E",
            participant_count = 1680, sort_order = 7, created_at = now
        ),
        ChallengeEntity(
            id = 8, title = "Ngủ trước 11 giờ tối — 30 ngày",
            description = "Đặt giấc ngủ làm ưu tiên: lên giường trước 23h trong 30 ngày liên tiếp. Một thói quen sẽ thay đổi mọi thói quen khác.",
            short_description = "Ngủ trước 11PM",
            motivational_quote = "Giấc ngủ là cách rẻ tiền nhất để trở nên thông minh hơn ngày mai.",
            completion_message = "Bạn vừa mua lại 30 buổi sáng tỉnh táo — và còn nhiều hơn thế.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 200, reward_badge_id = 1,
            icon_emoji = "🌙", color_hex = "#6366F1",
            participant_count = 1980, sort_order = 8, created_at = now
        ),

        // =====================================================================
        // 2. HEALTH & FITNESS (8) — reward_badge_id 6..10
        // =====================================================================
        ChallengeEntity(
            id = 9, title = "10.000 bước mỗi ngày — 30 ngày",
            description = "Mỗi ngày 10.000 bước — đó là bài thuốc đơn giản nhất cho sức khỏe tim mạch và tinh thần.",
            short_description = "10K bước/ngày trong 30 ngày",
            motivational_quote = "Mỗi bước chân là một lá phiếu cho phiên bản tốt hơn của bạn.",
            completion_message = "300.000 bước. Một con người mới được sinh ra từ những bước chân đó.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 220, reward_badge_id = 10,
            icon_emoji = "🏃", color_hex = "#F97316", is_featured = true,
            participant_count = 7240, sort_order = 9, created_at = now
        ),
        ChallengeEntity(
            id = 10, title = "Tập luyện đều đặn 21 ngày",
            description = "21 ngày liên tục có ít nhất 30 phút vận động — gym, chạy bộ, yoga, hay tại nhà đều được. Quan trọng là không nghỉ.",
            short_description = "30 phút vận động/ngày",
            motivational_quote = "Cơ thể đạt được điều mà tâm trí tin rằng có thể.",
            completion_message = "Cơ bắp và ý chí của bạn vừa cùng nhau lên cấp.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 200, reward_badge_id = 6,
            icon_emoji = "💪", color_hex = "#EF4444", is_featured = true,
            participant_count = 5400, sort_order = 10, created_at = now
        ),
        ChallengeEntity(
            id = 11, title = "Không ăn đường 14 ngày",
            description = "Cắt hoàn toàn đường tinh chế trong 14 ngày — một reset dữ dội cho vị giác và năng lượng.",
            short_description = "14 ngày không đường",
            motivational_quote = "Đường là cám dỗ ngọt ngào nhất — bạn vừa từ chối nó.",
            completion_message = "Vị giác đã khôi phục, năng lượng ổn định, và cơn thèm đã hạ xuống.",
            category_id = 2, difficulty = "HARD",
            duration_days = 14, target_streak = 14, reward_coins = 220, reward_badge_id = 9,
            icon_emoji = "🍩", color_hex = "#EC4899", is_featured = true,
            participant_count = 2100, sort_order = 11, created_at = now
        ),
        ChallengeEntity(
            id = 12, title = "Tập tại nhà liên tục 30 ngày",
            description = "Không cần phòng gym — 20 phút bài tập tại nhà mỗi ngày trong 30 ngày để xây dựng nền tảng thể lực.",
            short_description = "30 ngày tập tại nhà",
            motivational_quote = "Phòng gym tốt nhất là phòng gym bạn có thể tới mỗi ngày.",
            completion_message = "30 ngày, không bỏ cuộc. Cơ thể của bạn không còn như cũ.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 240, reward_badge_id = 6,
            icon_emoji = "🏋️", color_hex = "#DC2626",
            participant_count = 3120, sort_order = 12, created_at = now
        ),
        ChallengeEntity(
            id = 13, title = "Reset chế độ ăn lành mạnh 21 ngày",
            description = "21 ngày tập trung vào thực phẩm nguyên chất, ít chế biến — để cơ thể nhớ lại cảm giác sạch và nhẹ.",
            short_description = "21 ngày ăn sạch",
            motivational_quote = "Thực phẩm là loại thuốc rẻ nhất bạn dùng mỗi ngày.",
            completion_message = "Cơ thể bạn vừa được khởi động lại từ bên trong.",
            category_id = 2, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 180, reward_badge_id = 9,
            icon_emoji = "🥗", color_hex = "#10B981",
            participant_count = 1450, sort_order = 13, created_at = now
        ),
        ChallengeEntity(
            id = 14, title = "Chiến binh 5 giờ sáng — 30 ngày",
            description = "30 ngày dậy lúc 5h sáng + tập luyện ngay lập tức. Đây là thử thách dành cho những ai muốn chiếm lĩnh ngày trước khi nó bắt đầu.",
            short_description = "Dậy 5AM + workout",
            motivational_quote = "Khi cả thế giới còn ngủ, bạn đã đang xây dựng đế chế của mình.",
            completion_message = "30 ngày hy sinh giấc ngủ ngon đổi lấy 30 ngày sống mãnh liệt.",
            category_id = 1, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 7,
            icon_emoji = "🥊", color_hex = "#9333EA",
            participant_count = 540, sort_order = 14, created_at = now
        ),
        ChallengeEntity(
            id = 15, title = "Cardio mỗi ngày — 14 ngày",
            description = "14 ngày liên tục có ít nhất 20 phút cardio — chạy bộ, đạp xe, nhảy dây hoặc bơi.",
            short_description = "20 phút cardio/ngày",
            motivational_quote = "Tim bạn không bao giờ phàn nàn khi được rèn luyện.",
            completion_message = "Tim của bạn vừa khỏe hơn — và bạn cũng vậy.",
            category_id = 1, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 160, reward_badge_id = 10,
            icon_emoji = "🏃‍♀️", color_hex = "#F59E0B",
            participant_count = 2860, sort_order = 15, created_at = now
        ),
        ChallengeEntity(
            id = 16, title = "Cải tổ toàn thân — 90 ngày",
            description = "90 ngày toàn diện: vận động, dinh dưỡng, ngủ đủ. Cam kết với phiên bản tốt nhất của bạn — và cam kết với bản thân.",
            short_description = "90 ngày làm mới cơ thể",
            motivational_quote = "Một năm sau bạn sẽ ước gì hôm nay đã bắt đầu.",
            completion_message = "Bạn vừa dành 90 ngày tốt nhất của mình cho chính mình.",
            category_id = 1, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 1000, reward_badge_id = 7,
            icon_emoji = "🔱", color_hex = "#EAB308", is_featured = true,
            participant_count = 320, sort_order = 16, created_at = now
        ),

        // =====================================================================
        // 3. LEARNING & PRODUCTIVITY (8) — reward_badge_id 11..15
        // =====================================================================
        ChallengeEntity(
            id = 17, title = "Đọc 10 trang sách mỗi ngày — 21 ngày",
            description = "Chỉ 10 trang. Cộng dồn lại sau 21 ngày bạn đã đọc xong cả một cuốn sách. Thói quen đơn giản, kết quả lớn.",
            short_description = "10 trang/ngày trong 21 ngày",
            motivational_quote = "Người đọc sách thay vì lướt mạng đang chiến thắng cuộc đua thầm lặng.",
            completion_message = "Bạn vừa đọc xong cuốn sách mà 'phiên bản trước' của bạn không bao giờ mở.",
            category_id = 4, difficulty = "EASY",
            duration_days = 21, target_streak = 21, reward_coins = 120, reward_badge_id = 11,
            icon_emoji = "📚", color_hex = "#8B5CF6",
            participant_count = 4520, sort_order = 17, created_at = now
        ),
        ChallengeEntity(
            id = 18, title = "Học 2 giờ mỗi ngày — 30 ngày",
            description = "30 ngày tập trung học sâu 2 giờ mỗi ngày. Đây là cách bạn xây dựng kỹ năng đáng giá.",
            short_description = "2 giờ học mỗi ngày",
            motivational_quote = "60 giờ tập trung có giá trị hơn 600 giờ phân tâm.",
            completion_message = "60 giờ đầu tư vào bản thân — kết quả sẽ trả về theo cấp số nhân.",
            category_id = 4, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 320, reward_badge_id = 14,
            icon_emoji = "🎓", color_hex = "#0EA5E9",
            participant_count = 1280, sort_order = 18, created_at = now
        ),
        ChallengeEntity(
            id = 19, title = "Deep Work Sprint — 14 ngày",
            description = "14 ngày tập trung 90 phút mỗi ngày, không xao nhãng — không điện thoại, không thông báo, không tab khác.",
            short_description = "90p deep work/ngày",
            motivational_quote = "Khả năng tập trung sâu là siêu năng lực thế kỷ 21.",
            completion_message = "Não của bạn vừa được rèn luyện như một cơ bắp — và nó đã mạnh hơn.",
            category_id = 4, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 200, reward_badge_id = 13,
            icon_emoji = "🎯", color_hex = "#0EA5E9",
            participant_count = 980, sort_order = 19, created_at = now
        ),
        ChallengeEntity(
            id = 20, title = "Tuần lễ không trì hoãn — 7 ngày",
            description = "7 ngày làm ngay những việc bạn đã trì hoãn. Không có gì là 'để mai', tất cả là 'làm bây giờ'.",
            short_description = "7 ngày zero procrastination",
            motivational_quote = "Cách tốt nhất để giết thói quen trì hoãn là không cho nó cơ hội thứ hai.",
            completion_message = "Bạn vừa phá vỡ vòng lặp trì hoãn — danh sách to-do giờ ngắn hơn tâm trí bạn.",
            category_id = 4, difficulty = "MEDIUM",
            duration_days = 7, target_streak = 7, reward_coins = 110, reward_badge_id = 13,
            icon_emoji = "⚡", color_hex = "#FBBF24",
            participant_count = 1620, sort_order = 20, created_at = now
        ),
        ChallengeEntity(
            id = 21, title = "Coding 1 giờ mỗi ngày — 30 ngày",
            description = "30 ngày luyện code mỗi ngày — algorithm, side project, hay ngôn ngữ mới. Quan trọng là viết code, không phải xem video.",
            short_description = "1h code/ngày trong 30 ngày",
            motivational_quote = "Code không tự viết — bạn cũng vậy.",
            completion_message = "Bạn không còn 'muốn học lập trình' — bạn ĐÃ là một lập trình viên.",
            category_id = 4, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 15,
            icon_emoji = "💻", color_hex = "#3B82F6",
            participant_count = 720, sort_order = 21, created_at = now
        ),
        ChallengeEntity(
            id = 22, title = "Học một kỹ năng mới — 60 ngày",
            description = "60 ngày cam kết với một kỹ năng mới: ngôn ngữ, nhạc cụ, vẽ, viết, nấu ăn... Đủ thời gian để vượt qua giai đoạn 'người mới khó chịu'.",
            short_description = "60 ngày kỹ năng mới",
            motivational_quote = "Người chưa biết hôm nay là người sẽ biết ngày mai — nếu bạn bắt đầu.",
            completion_message = "60 ngày trước bạn nói 'tôi không biết'. Hôm nay bạn nói 'để tôi chỉ cho bạn'.",
            category_id = 4, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 600, reward_badge_id = 14,
            icon_emoji = "🚀", color_hex = "#7C3AED", is_featured = true,
            participant_count = 480, sort_order = 22, created_at = now
        ),
        ChallengeEntity(
            id = 23, title = "Viết nhật ký mỗi ngày — 21 ngày",
            description = "21 ngày viết nhật ký mỗi tối — 5 phút phản chiếu về những gì bạn đã làm, đã học, đã cảm.",
            short_description = "5 phút nhật ký/tối",
            motivational_quote = "Cuộc đời không được kiểm chứng là cuộc đời chưa được sống — Socrates.",
            completion_message = "21 trang nhật ký giờ là tấm gương trung thực nhất bạn từng nhìn.",
            category_id = 3, difficulty = "EASY",
            duration_days = 21, target_streak = 21, reward_coins = 120, reward_badge_id = 11,
            icon_emoji = "📝", color_hex = "#A855F7",
            participant_count = 1980, sort_order = 23, created_at = now
        ),
        ChallengeEntity(
            id = 24, title = "Chinh phục năng suất — 90 ngày",
            description = "90 ngày làm chủ thời gian: time-blocking, deep work, không xao nhãng. Đỉnh cao của kỷ luật năng suất.",
            short_description = "90 ngày năng suất tối ưu",
            motivational_quote = "Năng suất không phải làm nhiều hơn — mà là làm đúng việc, mỗi ngày.",
            completion_message = "Bạn không quản lý thời gian nữa — bạn sở hữu nó.",
            category_id = 4, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 900, reward_badge_id = 15,
            icon_emoji = "👑", color_hex = "#7C3AED",
            participant_count = 240, sort_order = 24, created_at = now
        ),

        // =====================================================================
        // 4. MENTAL WELLNESS (8) — reward_badge_id 26..30
        // =====================================================================
        ChallengeEntity(
            id = 25, title = "Thiền mỗi ngày — 21 ngày",
            description = "10 phút thiền chánh niệm mỗi sáng trong 21 ngày — đủ để tâm trí học cách tự tĩnh lại.",
            short_description = "10 phút thiền/ngày",
            motivational_quote = "Tâm trí giống như nước — khi yên, nó phản chiếu sự thật.",
            completion_message = "Bạn vừa tìm thấy một nơi yên tĩnh bên trong — và biết cách quay về đó.",
            category_id = 3, difficulty = "EASY",
            duration_days = 21, target_streak = 21, reward_coins = 130, reward_badge_id = 28,
            icon_emoji = "🧘", color_hex = "#14B8A6", is_featured = true,
            participant_count = 3640, sort_order = 25, created_at = now
        ),
        ChallengeEntity(
            id = 26, title = "Nhật ký biết ơn — 14 ngày",
            description = "Mỗi tối, ghi 3 điều bạn biết ơn. 14 ngày để rèn luyện cơ bắp tích cực của tâm trí.",
            short_description = "3 điều biết ơn/tối",
            motivational_quote = "Lòng biết ơn biến những gì ta có thành đủ.",
            completion_message = "Tâm trí của bạn giờ tự tìm thấy ánh sáng — kể cả những ngày mây.",
            category_id = 3, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 100, reward_badge_id = 27,
            icon_emoji = "🙏", color_hex = "#F472B6",
            participant_count = 2480, sort_order = 26, created_at = now
        ),
        ChallengeEntity(
            id = 27, title = "Detox kỹ thuật số buổi tối — 21 ngày",
            description = "21 ngày không dùng điện thoại sau 9h tối. Lấy lại buổi tối — đọc sách, nói chuyện, hoặc chỉ đơn giản là ngủ.",
            short_description = "Off điện thoại sau 9PM",
            motivational_quote = "Buổi tối là khi tâm trí cần nghỉ — không phải khi nó cần nhiều thông tin hơn.",
            completion_message = "Bạn vừa lấy lại 21 buổi tối — và giấc ngủ chất lượng đi kèm.",
            category_id = 3, difficulty = "MEDIUM",
            duration_days = 21, target_streak = 21, reward_coins = 180, reward_badge_id = 26,
            icon_emoji = "📵", color_hex = "#0EA5E9",
            participant_count = 1320, sort_order = 27, created_at = now
        ),
        ChallengeEntity(
            id = 28, title = "Tư duy tích cực mỗi ngày — 30 ngày",
            description = "Mỗi sáng, viết một câu khẳng định tích cực về bản thân và mục tiêu. 30 ngày để tái lập trình giọng nói nội tâm.",
            short_description = "1 câu tích cực/sáng",
            motivational_quote = "Bạn nghe giọng nói của chính mình nhiều nhất — hãy dạy nó nói tử tế.",
            completion_message = "Giọng nói nội tâm của bạn vừa được nâng cấp.",
            category_id = 3, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 200, reward_badge_id = 29,
            icon_emoji = "☀️", color_hex = "#FBBF24",
            participant_count = 980, sort_order = 28, created_at = now
        ),
        ChallengeEntity(
            id = 29, title = "Buổi sáng không stress — 14 ngày",
            description = "14 ngày bắt đầu ngày bằng một quy trình bình tĩnh: không kiểm tra điện thoại trong 30 phút đầu, một bài thiền ngắn, một bữa sáng chậm rãi.",
            short_description = "Morning routine bình tĩnh",
            motivational_quote = "Buổi sáng vội là cả ngày vội — buổi sáng tĩnh là cả ngày tĩnh.",
            completion_message = "Bạn đã đặt nền móng cho 14 ngày bình yên — và biết cách lặp lại.",
            category_id = 3, difficulty = "EASY",
            duration_days = 14, target_streak = 14, reward_coins = 120, reward_badge_id = 30,
            icon_emoji = "🌸", color_hex = "#F9A8D4",
            participant_count = 1140, sort_order = 29, created_at = now
        ),
        ChallengeEntity(
            id = 30, title = "Reset chánh niệm — 30 ngày",
            description = "30 ngày sống chậm: ăn không đa nhiệm, đi bộ không nghe gì, lắng nghe sâu khi nói chuyện.",
            short_description = "30 ngày sống chánh niệm",
            motivational_quote = "Nơi tâm trí bạn ở là nơi cuộc đời bạn diễn ra.",
            completion_message = "Bạn vừa học được điều quý nhất: cách hiện diện hoàn toàn ở khoảnh khắc này.",
            category_id = 3, difficulty = "MEDIUM",
            duration_days = 30, target_streak = 30, reward_coins = 220, reward_badge_id = 28,
            icon_emoji = "🪷", color_hex = "#A78BFA",
            participant_count = 760, sort_order = 30, created_at = now
        ),
        ChallengeEntity(
            id = 31, title = "Nghỉ mạng xã hội — 21 ngày",
            description = "21 ngày không Instagram, Facebook, TikTok, X. Lấy lại bộ não, lấy lại sự tập trung, lấy lại bạn.",
            short_description = "21 ngày off social",
            motivational_quote = "Sự chú ý là tài nguyên có hạn — đừng cho không.",
            completion_message = "Đầu óc bạn vừa được trả về cho chính bạn. Đừng bán nó rẻ nữa.",
            category_id = 3, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 280, reward_badge_id = 26,
            icon_emoji = "🚫", color_hex = "#0F172A",
            participant_count = 540, sort_order = 31, created_at = now
        ),
        ChallengeEntity(
            id = 32, title = "Hành trình bình an nội tâm — 60 ngày",
            description = "60 ngày tổng hợp: thiền, biết ơn, sống chậm, nghỉ mạng xã hội. Một hành trình chuyển hóa tâm trí từ bên trong.",
            short_description = "60 ngày chinh phục bình an",
            motivational_quote = "Bình an không phải một nơi — đó là một thực hành.",
            completion_message = "60 ngày trước bạn tìm bình an. Hôm nay bạn LÀ bình an.",
            category_id = 3, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 700, reward_badge_id = 30,
            icon_emoji = "🕉️", color_hex = "#7C3AED", is_featured = true,
            participant_count = 240, sort_order = 32, created_at = now
        ),

        // =====================================================================
        // 5. DISCIPLINE & CONSISTENCY (8) — reward_badge_id 21..25
        // =====================================================================
        ChallengeEntity(
            id = 33, title = "Không dậy muộn — 30 ngày",
            description = "30 ngày liên tiếp dậy đúng giờ đã định, không snooze. Đây là bài kiểm tra cốt lõi của ý chí.",
            short_description = "30 ngày zero snooze",
            motivational_quote = "Snooze là hợp đồng đầu tiên bạn phá với chính mình mỗi ngày.",
            completion_message = "Bạn vừa thắng 30 trận đấu với chính mình — và mỗi trận đều quan trọng.",
            category_id = 5, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 280, reward_badge_id = 21,
            icon_emoji = "⏰", color_hex = "#FB923C",
            participant_count = 1820, sort_order = 33, created_at = now
        ),
        ChallengeEntity(
            id = 34, title = "Dopamine Detox — 14 ngày",
            description = "14 ngày cai mọi nguồn dopamine ngắn: không Reels, không TikTok, không trò chơi gây nghiện, không snack.",
            short_description = "14 ngày detox dopamine",
            motivational_quote = "Não bạn được thiết kế để theo đuổi mục tiêu lớn — đừng để nó nghiện những phần thưởng nhỏ.",
            completion_message = "Bạn vừa lấy lại quyền kiểm soát hệ thống thưởng của não bộ.",
            category_id = 5, difficulty = "HARD",
            duration_days = 14, target_streak = 14, reward_coins = 240, reward_badge_id = 22,
            icon_emoji = "🧠", color_hex = "#0EA5E9",
            participant_count = 980, sort_order = 34, created_at = now
        ),
        ChallengeEntity(
            id = 35, title = "Không đồ ăn nhanh — 21 ngày",
            description = "21 ngày không đồ chiên rán, không fast food, không snack đóng gói. Chỉ thức ăn được nấu nướng tử tế.",
            short_description = "21 ngày no junk food",
            motivational_quote = "Cơ thể bạn không phải thùng rác — đừng ném vào đó những gì rẻ tiền.",
            completion_message = "Vị giác và năng lượng của bạn vừa được nâng cấp.",
            category_id = 2, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 220, reward_badge_id = 21,
            icon_emoji = "🍔", color_hex = "#DC2626",
            participant_count = 1240, sort_order = 35, created_at = now
        ),
        ChallengeEntity(
            id = 36, title = "Lịch trình kỷ luật — 30 ngày",
            description = "30 ngày tuân thủ một lịch trình ngày nghiêm ngặt: dậy đúng giờ, ngủ đúng giờ, làm việc đúng giờ. Không du di.",
            short_description = "30 ngày lịch trình cố định",
            motivational_quote = "Tự do thật sự đến từ kỷ luật — không phải từ sự tùy hứng.",
            completion_message = "Bạn vừa biến cuộc sống thành một hệ thống — và hệ thống đang chạy.",
            category_id = 5, difficulty = "HARD",
            duration_days = 30, target_streak = 30, reward_coins = 300, reward_badge_id = 22,
            icon_emoji = "📋", color_hex = "#475569",
            participant_count = 680, sort_order = 36, created_at = now
        ),
        ChallengeEntity(
            id = 37, title = "Không bỏ lỡ check-in — 60 ngày",
            description = "60 ngày liên tiếp không bỏ lỡ một ngày check-in nào — bất kể đó là thử thách gì. Một thử thách về kỷ luật thuần khiết.",
            short_description = "60 ngày zero miss",
            motivational_quote = "Kỷ luật là cây cầu giữa mục tiêu và thành tựu.",
            completion_message = "Bạn đã chứng minh điều đáng giá nhất: bạn có thể tin được vào chính mình.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 600, reward_badge_id = 24,
            icon_emoji = "🎯", color_hex = "#9333EA",
            participant_count = 320, sort_order = 37, created_at = now
        ),
        ChallengeEntity(
            id = 38, title = "Tắm nước lạnh — 21 ngày",
            description = "21 ngày tắm nước lạnh mỗi sáng. Một bài kiểm tra ý chí đơn giản nhưng mạnh mẽ — và tốt cho hệ miễn dịch.",
            short_description = "21 ngày cold shower",
            motivational_quote = "Nếu bạn có thể chiến thắng cơn lười tắm nước lạnh, bạn có thể chiến thắng bất cứ điều gì.",
            completion_message = "Cơ thể bạn vừa học được cách yêu cảm giác khó chịu.",
            category_id = 1, difficulty = "HARD",
            duration_days = 21, target_streak = 21, reward_coins = 250, reward_badge_id = 22,
            icon_emoji = "🥶", color_hex = "#06B6D4",
            participant_count = 460, sort_order = 38, created_at = now
        ),
        ChallengeEntity(
            id = 39, title = "Tập trung không xao nhãng — 14 ngày",
            description = "14 ngày làm việc với điện thoại ở chế độ máy bay trong giờ làm. Đo lường năng suất trước và sau.",
            short_description = "Phone airplane mode 14d",
            motivational_quote = "Bạn không có vấn đề về thời gian — bạn có vấn đề về sự xao nhãng.",
            completion_message = "Bạn vừa khám phá ra mình có thể làm được nhiều thứ đến thế nào khi tập trung.",
            category_id = 4, difficulty = "MEDIUM",
            duration_days = 14, target_streak = 14, reward_coins = 180, reward_badge_id = 21,
            icon_emoji = "🛡️", color_hex = "#475569",
            participant_count = 720, sort_order = 39, created_at = now
        ),
        ChallengeEntity(
            id = 40, title = "Bậc Thầy Kiên Trì — 90 ngày",
            description = "90 ngày liên tiếp không bỏ một thói quen tốt nào. Đây là cấp độ cuối của kỷ luật cá nhân.",
            short_description = "90 ngày kiên trì tuyệt đối",
            motivational_quote = "Kỷ luật là chọn cái bạn muốn nhất, không phải cái bạn muốn ngay bây giờ.",
            completion_message = "90 ngày sau, bạn không còn cần ý chí nữa — đã thành bản năng.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 900, reward_badge_id = 25,
            icon_emoji = "⚔️", color_hex = "#1E1B4B",
            participant_count = 180, sort_order = 40, created_at = now
        ),

        // =====================================================================
        // 6. SPECIAL / LEGENDARY (8) — reward_badge_id 16..20
        // =====================================================================
        ChallengeEntity(
            id = 41, title = "Monk Mode — 90 ngày",
            description = "90 ngày kỷ luật cực đoan — chỉ tập trung vào sức khỏe, công việc, học tập. Không giải trí, không mạng xã hội, không cám dỗ. Đây là Đại Đạo.",
            short_description = "90 ngày Monk Mode",
            motivational_quote = "90 ngày không có người ngoài — chỉ còn bạn và mục tiêu.",
            completion_message = "Phiên bản hiện tại của bạn được rèn từ 90 ngày tĩnh lặng.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 1200, reward_badge_id = 18,
            icon_emoji = "🛕", color_hex = "#1E1B4B", is_featured = true,
            participant_count = 220, sort_order = 41, created_at = now
        ),
        ChallengeEntity(
            id = 42, title = "Reset toàn diện — 60 ngày",
            description = "60 ngày làm mới mọi mảng đời sống: sức khỏe, mối quan hệ, tài chính, kỹ năng. Một cuộc lột xác hoàn chỉnh.",
            short_description = "60 ngày làm mới đời sống",
            motivational_quote = "Đôi khi bạn cần phá huỷ phiên bản cũ để xây dựng phiên bản tốt hơn.",
            completion_message = "Bạn của 60 ngày trước sẽ không nhận ra bạn của hôm nay.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 800, reward_badge_id = 16,
            icon_emoji = "🔄", color_hex = "#7C3AED", is_featured = true,
            participant_count = 380, sort_order = 42, created_at = now
        ),
        ChallengeEntity(
            id = 43, title = "Trở thành phiên bản tốt nhất của bạn — 90 ngày",
            description = "90 ngày cam kết toàn diện với phiên bản tốt nhất bạn có thể trở thành.",
            short_description = "90 ngày best self",
            motivational_quote = "Phiên bản tốt nhất của bạn đang chờ đợi 90 ngày kỷ luật của bạn.",
            completion_message = "Bạn vừa trở thành người mà bạn luôn biết mình có thể trở thành.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 90, target_streak = 90, reward_coins = 1100, reward_badge_id = 19,
            icon_emoji = "✨", color_hex = "#EAB308", is_featured = true,
            participant_count = 280, sort_order = 43, created_at = now
        ),
        ChallengeEntity(
            id = 44, title = "Cấm mạng xã hội cực đoan — 60 ngày",
            description = "60 ngày hoàn toàn không mạng xã hội nào — không Instagram, không Facebook, không TikTok, không X, không YouTube giải trí.",
            short_description = "60 ngày off all socials",
            motivational_quote = "Bạn dành cuộc đời mình ở đâu là cuộc đời mình ở đó.",
            completion_message = "60 ngày không mạng xã hội — bạn vừa lấy lại 1.000+ giờ cho cuộc đời.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 60, target_streak = 60, reward_coins = 700, reward_badge_id = 17,
            icon_emoji = "🔕", color_hex = "#0F172A",
            participant_count = 180, sort_order = 44, created_at = now
        ),
        ChallengeEntity(
            id = 45, title = "75 Hard Inspired — 75 ngày",
            description = "Lấy cảm hứng từ 75 Hard: 2 buổi tập 45 phút mỗi ngày, theo một chế độ ăn nghiêm ngặt, đọc 10 trang sách non-fiction, uống 4L nước, không cồn. Bỏ một ngày là bắt đầu lại.",
            short_description = "75 ngày 75 Hard rules",
            motivational_quote = "Chương trình huấn luyện ý chí khắc nghiệt nhất bạn từng thử.",
            completion_message = "Bạn vừa hoàn thành chương trình ý chí khắc nghiệt nhất hiện có. Mọi việc khác giờ trở nên dễ hơn.",
            category_id = 1, difficulty = "LEGENDARY",
            duration_days = 75, target_streak = 75, reward_coins = 1000, reward_badge_id = 20,
            icon_emoji = "🥇", color_hex = "#DC2626",
            participant_count = 140, sort_order = 45, created_at = now
        ),
        ChallengeEntity(
            id = 46, title = "Hành trình kỷ luật tối thượng — 100 ngày",
            description = "100 ngày kỷ luật toàn diện trên 5 mảng: thể chất, tinh thần, học tập, công việc, tài chính. Đỉnh cao của BetterMe.",
            short_description = "100 ngày Ultimate Discipline",
            motivational_quote = "Sau 100 ngày, không có thử thách nào có thể đe dọa bạn nữa.",
            completion_message = "Bạn không còn cố gắng kỷ luật — bạn LÀ kỷ luật.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 100, target_streak = 100, reward_coins = 1300, reward_badge_id = 18,
            icon_emoji = "🏛️", color_hex = "#7C3AED",
            participant_count = 90, sort_order = 46, created_at = now
        ),
        ChallengeEntity(
            id = 47, title = "Chuyển hóa huyền thoại — 120 ngày",
            description = "120 ngày — 4 tháng — của một cuộc chuyển hóa toàn diện. Đây là lựa chọn cho người không sợ độ dài.",
            short_description = "120 ngày Legendary",
            motivational_quote = "Người chỉ cam kết ngắn hạn không bao giờ đạt thành quả dài hạn.",
            completion_message = "120 ngày của bạn vừa viết nên một câu chuyện huyền thoại.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 120, target_streak = 120, reward_coins = 1500, reward_badge_id = 18,
            icon_emoji = "🐉", color_hex = "#1E40AF",
            participant_count = 60, sort_order = 47, created_at = now
        ),
        ChallengeEntity(
            id = 48, title = "BetterMe Elite Challenge — 180 ngày",
            description = "180 ngày — 6 tháng — của cam kết tổng lực. Đây là thử thách cuối cùng. Hoàn thành điều này, bạn không còn là phiên bản cũ.",
            short_description = "180 ngày BetterMe Elite",
            motivational_quote = "Tinh hoa không phải là sinh ra — đó là kết quả của 180 ngày.",
            completion_message = "Bạn vừa đặt mình vào hàng ngũ tinh hoa. Người đã hoàn thành thử thách này không còn là người bắt đầu nó.",
            category_id = 5, difficulty = "LEGENDARY",
            duration_days = 180, target_streak = 180, reward_coins = 2000, reward_badge_id = 18,
            icon_emoji = "👑", color_hex = "#EAB308", is_featured = true,
            participant_count = 28, sort_order = 48, created_at = now
        )
    )
}
