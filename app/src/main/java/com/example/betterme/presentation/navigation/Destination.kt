package com.example.betterme.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {

    @Serializable
    data object Splash : Destination

    @Serializable
    data object Welcome : Destination

    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object SignIn : Destination

    @Serializable
    data object Main : Destination
    @Serializable
    data object Settings : Destination

}

/*
Navigation 3 (navigation3) với NavDisplay + NavKey, đây là cách hiện đại và clean hơn nav-compose truyền thống.
🧱 BƯỚC 1 — Tạo file Destination.kt:
presentation/navigation/Destination.kt
@Serializable
sealed interface Destination : NavKey {

    @Serializable
    data object Splash : Destination
}
1️⃣ sealed interface Destination
👉 sealed
sealed nghĩa là chỉ được phép định nghĩa các implementation trong cùng file.
Tức là tất cả màn hình (Splash, Welcome, Main, SignIn…) bắt buộc nằm trong file này.
📌 Lợi ích:
Compiler biết toàn bộ các màn hình có thể có
Khi dùng when(destination) sẽ được check đầy đủ (không cần else)
Ví dụ:
when(destination) {
    Destination.Splash -> ...
    Destination.Main -> ...
}
Nếu bạn thêm màn hình mới mà chưa xử lý trong when, compiler sẽ báo lỗi.

2️⃣ interface Destination : NavKey
Bạn đang dùng:
androidx.navigation3.runtime.NavKey
NavKey là key đại diện cho một màn hình trong Navigation 3.
Tức là:
Destination chính là định nghĩa tất cả các route
Mỗi object bên trong là một màn hình

3️⃣ data object Splash : Destination
Đây là cú pháp mới của Kotlin.
🔹 object
→ nghĩa là chỉ có một instance duy nhất
→ Phù hợp cho màn hình không có tham số (Splash, Main…)
Tương đương kiểu:
object Splash : Destination
🔹 data object
Giống data class nhưng cho object
Tự động có:
toString()
equals()
hashCode()
Navigation cần so sánh object → nên dùng data object là chuẩn.

4️⃣ @Serializable
Bạn đang dùng:
import kotlinx.serialization.Serializable

Navigation 3 dùng serialization để:
Lưu state khi xoay màn hình
Lưu back stack
Restore khi process bị kill
Vì vậy tất cả Destination phải có @Serializable.
Nếu không có sẽ crash khi app restore state.

💡 Tóm lại dòng này có ý nghĩa:
@Serializable
data object Splash : Destination

Nó có nghĩa là:
"Splash là một màn hình trong hệ thống navigation, chỉ có một instance duy nhất, và có thể được serialize để lưu state."

2️⃣ Định nghĩa từng màn hình
🟢 Màn hình KHÔNG có tham số
@Serializable
data object Splash : Destination
Giải thích:
Thành phần	Ý nghĩa
data object	Chỉ có 1 instance duy nhất
Splash	Tên màn hình
: Destination	Đây là 1 NavKey
👉 Dùng cho:
Splash
Welcome
Main
Settings

🔵 Màn hình CÓ tham số
@Serializable
data class PracticeQuestion(val topicId: String) : Destination
Giải thích:
Thành phần	Ý nghĩa
data class	Vì có dữ liệu
val topicId: String	Param truyền qua màn hình
: Destination	Là NavKey
👉 Dùng khi:
Cần truyền ID
Cần truyền number
Cần truyền object

🧩 BƯỚC 2: Tạo NavRoutes (Navigation Host)

📁 File: navigation/NavRoutes.kt

1️⃣ Tạo backStack
val backStack = rememberNavBackStack(Destination.Splash)
Ý nghĩa:
Thành phần	                    Ý nghĩa
rememberNavBackStack	    Tạo stack điều hướng
Destination.Splash	        Màn hình bắt đầu
👉 Tương đương startDestination

2️⃣ NavDisplay
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    entryProvider = entryProvider {
Giải thích từng phần
🔹 backStack = backStack
Truyền stack vào hệ thống navigation

🔹 onBack
onBack = { backStack.removeLastOrNull() }
→ Khi bấm nút back hệ thống
→ Xóa màn hình cuối khỏi stack

🔹 entryDecorators
rememberSaveableStateHolderNavEntryDecorator()
Giữ state khi xoay màn hình

rememberViewModelStoreNavEntryDecorator()
Giữ ViewModel theo từng màn hình

🔥 Cái này cực kỳ quan trọng

3️⃣ Định nghĩa từng màn hình
entry<Destination.Splash> {
    SplashScreen(
        navigateToMain = {
            backStack.replaceTop(Destination.Main)
        }
    )
}
Giải thích
Thành phần	                    Ý nghĩa
entry<Destination.Splash>	Khi key là Splash
SplashScreen()	            Hiển thị UI
backStack.replaceTop()	    Thay màn hình hiện tại

🧩 BƯỚC 3: Các loại điều hướng
🔹 1. Thay màn hình (không quay lại được)
backStack.replaceTop(Destination.Main)
👉 Dùng cho:
Splash → Main
Login → Main
Logout → SignIn

🔹 2. Thêm màn hình (có back)
backStack.add(Destination.Settings)

👉 Dùng cho:
Main → Settings
Main → Detail

🔹 3. Quay lại
backStack.removeLastOrNull()
🧩 BƯỚC 4: Utils replaceTop

📁 File: utils/replaceTop.kt

Tạo extension
fun <T> MutableList<T>.replaceTop(newItem: T) {
    if (this.isNotEmpty()) {
        this[this.lastIndex] = newItem
    } else {
        add(newItem)
    }
}
Giải thích
Thành phần	            Ý nghĩa
MutableList<T>	    Vì backStack là List
replaceTop	        Thay phần tử cuối
lastIndex	        Vị trí cuối stack

👉 Giúp code sạch hơn thay vì:

backStack[backStack.lastIndex] = Destination.Main
🧠 BƯỚC 5: Cách Navigation 3 hoạt động

Ví dụ flow:
Splash

Stack:

[ Splash ]

Navigate sang Main bằng replaceTop

[ Main ]

Navigate sang Settings bằng add

[ Main, Settings ]

Bấm back

[ Main ]
🏗 BƯỚC 6: Cấu trúc chuẩn cho project lớn
navigation/
    Destination.kt
    NavRoutes.kt
utils/
    NavKeyExt.kt

presentation/
    splash/
    signin/
    main/
    settings/
👉 Navigation không được đặt trong presentation
👉 Navigation là layer riêng

🎯 BƯỚC 7: Flow login-google chuẩn
Với project của bạn, flow nên là:

Splash
   ↓
Welcome
   ↓
Onboarding
   ↓
SignIn (Google login)
   ↓
Main
Logout:
Main → Settings → logout → SignIn
🚀 Tại sao cách bạn làm là chuẩn?

✔ Type-safe
✔ Không dùng string route
✔ Không crash vì sai param
✔ ViewModel scoped đúng
✔ Dễ test
✔ Clean Architecture friendly

🔥 Tóm tắt 3 bước chính
Bước 1
Tạo Destination.kt
Bước 2
Tạo NavRoutes.kt
Bước 3
Tạo replaceTop.kt
*/