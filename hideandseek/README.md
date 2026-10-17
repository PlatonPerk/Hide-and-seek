# Прятунчики — «Прятки» поверх приложения

Раз в день зверёк прячется в одном из разрешённых мест приложения и выглядывает из-за края элемента: карточки баланса, кнопок, нижнего меню. Ребёнок замечает его и тапает — «Нашёл!»: зверёк выпрыгивает, разлетаются звёздочки, появляется карточка с советом про деньги и серией дней. Место **одинаковое у всех детей в этот день** (выбор детерминированный, по дате), поэтому о нём можно рассказать друзьям.

- Это слой поверх, а не экран: касания проходят сквозь него, кроме видимой части зверька.
- На экранах оплаты и переводов зверька не бывает, даже карточка «Нашёл!» сразу скрывается.
- Весь контент — один JSON: зверьки, места, веса, расписание, часы, тексты, советы, особые дни, выключатель.
- Без бэкенда и новых разрешений: JSON по ключу CMS или статикой по https, прогресс — на устройстве.

## Что внутри

```
hideandseek/                 библиотека — её и встраивают в приложение
  core/                      чистый Kotlin без Android: JSON, даты, расписание, выбор дня
  HideAndSeek.kt             API: контроллер, HideAndSeekHost, Modifier.hidingSpot, HideAndSeekBlocker
  Overlay.kt                 слой: выглядывающий зверёк, «Нашёл!», карточка с советом
  CreatureArt.kt             зверёк, нарисованный кодом (или PNG из контента)
  web/HidingSpotsWebView.kt  мост для сайта-имитации
demo/                        демо-приложение: нативные экраны-моки, вкладка «Сайт», панель демо
content/hideandseek.json     контент для CDN (GitHub Pages)
```

Зависимости библиотеки — только Compose UI и Foundation. Картинки, сеть и JSON — без сторонних библиотек.

## Запуск демо

1. Открыть папку в Android Studio, дождаться Gradle Sync, запустить конфигурацию `demo`.
2. На главной нажать ⚙ — откроется панель демо: где прячется зверёк сегодня, «◀ день / день ▶» (перемотка дней), «Спрятать снова», «Обновить контент».
3. Вкладка «Сайт» открывает сайт-имитацию в WebView — зверёк работает и поверх него.
4. «Перевести» на главной открывает защищённый экран: там зверька нет.

## Встраивание в любое Compose-приложение — 3 шага

Вариант А: подключить модуль `hideandseek` (`include(":hideandseek")` и `implementation(project(":hideandseek"))`).
Вариант Б (быстрее): скопировать папку `hideandseek/src/main/java/app/hideandseek` в свой модуль.

```kotlin
// 1. Один раз в корне приложения
val hideAndSeek = rememberHideAndSeekController(
    contentUrl = "https://<user>.github.io/<repo>/hideandseek.json",
)
HideAndSeekHost(hideAndSeek, Modifier.fillMaxSize()) {
    AppNavHost() // всё приложение как есть
}

// 2. На любом элементе, за которым можно прятаться
BalanceCard(modifier = Modifier.hidingSpot("home.balance"))

// 3. На экранах, которые должны оставаться чистыми (оплата, переводы, реквизиты, лимиты)
@Composable
fun TransferScreen() {
    HideAndSeekBlocker()
    // …
}
```

- `id` в `hidingSpot("…")` должен совпадать с `id` места в JSON. Места, которых нет в JSON, просто не используются.
- В продакшене контент берётся по ключу CMS: `rememberHideAndSeekController(source = ContentSource { cms.getJson("kids_hide_and_seek") })`.
- Если JSON не задан или не скачался, используется кеш, а при первом запуске — копия из `assets/hideandseek/content.json`.
- Аналитика: `hideAndSeek.onFound = { plan -> analytics.log("hide_seek_found", plan.creature.id) }`.

## Сайт-имитация

Сайт открывается во вкладке «Сайт» демо-приложения, а зверёк — тот же нативный слой поверх WebView. Логика, контент и анимации общие с нативными экранами, поэтому код без изменений переносится в настоящее приложение.

1. Укажите адрес в `demo/.../MainActivity.kt` → `DemoConfig.WEBSITE_URL`. Для локального сервера: `http://<IP компьютера>:<порт>/`, телефон и компьютер в одной Wi-Fi сети.
2. Отметьте места на страницах:
   ```html
   <div class="balance" data-hide-spot="home.balance">…</div>
   ```
   или, не трогая сайт, задайте CSS-селектор в JSON: `{ "id": "home.goal", "web": ".goal-card" }`.
3. На страницах оплаты добавьте атрибут любому видимому элементу:
   ```html
   <div data-hide-seek-block>…форма перевода…</div>
   ```
4. В `<head>` должен быть `<meta name="viewport" content="width=device-width, initial-scale=1">`.

Пример размеченной страницы: `demo/src/main/assets/site/index.html`. Мост сам отслеживает прокрутку, изменения DOM и переходы внутри SPA.

## Контент без релиза (GitHub Pages вместо CMS)

1. Создайте публичный репозиторий, положите в него `content/hideandseek.json`, включите Settings → Pages.
2. Вставьте ссылку на файл в `DemoConfig.CONTENT_URL`.
3. Отредактируйте JSON на GitHub, подождите около минуты, нажмите «Обновить контент» в панели демо (или перезапустите приложение) — зверёк, место или тексты поменяются без сборки.

Чтобы на защите зверёк точно был в нужном месте, добавьте особый день:
`{ "date": "2026-10-18", "creature": "zolotik", "spot": "home.balance" }`.

| Поле | Что делает |
| --- | --- |
| `enabled` | выключатель фичи без релиза |
| `timeZone` | по какому времени считается «сегодня» (у всех одинаково, по умолчанию Москва) |
| `salt` | если поменять, все будущие дни перетасуются |
| `schedule.startDate`, `endDate` | период работы |
| `schedule.daysOfWeek` | дни недели: числа 1–7 или "пн"…"вс" |
| `schedule.everyNDays` | прятаться раз в N дней |
| `schedule.chancePercent` | шанс, что зверёк вообще спрячется в этот день (одинаковый у всех) |
| `schedule.hours` | часы, когда он может появиться (`from`–`to`) |
| `schedule.avoidRepeat` | не прятаться в одном месте два дня подряд |
| `look` | `sizeDp`, `peek` (насколько выглядывает, 0..1) — для зверька без поз; `appearDelayMs`, `messageMs` |
| `creatures[]` | `id`, `name`, `color`, `weight`, `rare`, `from`/`to` (сезонные), `image` (PNG по https или `asset:путь`) |
| `creatures[].sprites` | позы-картинки: `peek` (сверху), `side` (сбоку), `found`, `celebrate`, `run`; у каждой `image`, `blink` (кадр моргания), `heightDp`, `anchor: [x, y]` (доли картинки) |
| `creatures[].spritesFrom` | взять позы у другого зверька по `id` |
| `spots[]` | `id`, `edge` (top / bottom / left / right), `align` (0..1 вдоль края), `weight`, `web` (селектор для сайта), `from`/`to` |
| `tips[]` | советы про деньги: строка или `{ "text", "from", "to" }` |
| `specialDays[]` | `date` и `creature` / `spot` / `tip` — праздники и демо; `"skip": true` — выходной |
| `texts` | тексты карточки; подстановки `{name}`, `{streak}`, `{streakDays}`, `{total}` |

## Почему это не мешает сценариям (для защиты)

| Ограничение организаторов | Как сделано |
| --- | --- |
| Слой поверх, а не экран | `HideAndSeekHost` рисует отдельный слой; сам слой касания не перехватывает |
| Не блокировать ввод | касания берёт только видимая часть зверька (не меньше 48 dp, снаружи элемента); элемент под ним работает целиком; карточка «Нашёл!» закрывается сама и по тапу |
| Не перекрывать оплату, переводы, реквизиты, лимиты | `HideAndSeekBlocker()` на экране или `data-hide-seek-block` на сайте; при переходе туда скрывается всё, включая карточку |
| Не замедлять запуск | контент и прогресс читаются в фоне после первого кадра, сеть — после них |
| Контент скачивается и кешируется | кеш в `filesDir` + встроенная копия в assets; битый JSON игнорируется |
| Поведение — настройками и весами | веса мест и зверьков, расписание, часы, шанс, особые дни, выключатель — всё в JSON |
| Без бэкенда | новый ключ CMS или статический файл; прогресс в SharedPreferences; новых разрешений нет |

Обучающая польза: каждая находка приносит совет дня про деньги (что такое кешбэк, зачем копилка, никому не называть код из СМС), серия дней мотивирует заходить, а коллекция зверьков — показывать друзьям.

## iOS (для слайдов)

Логика из `core/` — чистый Kotlin, её можно отдать на iOS через Kotlin Multiplatform или переписать на Swift (без своего парсера JSON это около 300 строк). Выбор дня на iOS и Android совпадёт, если повторить алгоритм хеша; контрольные значения: `hash("") = 0xAB3E7C0B`, `hash("hello") = 0x888D766E` (тест `CoreTest.hashIsStableAcrossPlatforms`).

Эскиз интерфейса на SwiftUI (не компилировался, для иллюстрации):

```swift
struct SpotFramesKey: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] = [:]
    static func reduce(value: inout [String: Anchor<CGRect>], nextValue: () -> [String: Anchor<CGRect>]) {
        value.merge(nextValue()) { $1 }
    }
}

extension View {
    // Аналог Modifier.hidingSpot("home.balance")
    func hidingSpot(_ id: String) -> some View {
        anchorPreference(key: SpotFramesKey.self, value: .bounds) { [id: $0] }
    }
}

// Аналог HideAndSeekHost: слой поверх корня, касания берёт только CreatureView
RootView()
    .overlayPreferenceValue(SpotFramesKey.self) { spots in
        GeometryReader { proxy in
            if let plan = controller.plan, let anchor = spots[plan.spotId] {
                CreatureView(plan: plan, spotFrame: proxy[anchor])
            }
        }
    }
```

## Что проверено и что нет

- `core/`: 12 юнит-тестов (`./gradlew :hideandseek:test`) — даты против `java.time`, хеш против независимой реализации, веса, «не два дня подряд», расписания, серии, JSON. Все проходят.
- Скрипт моста для сайта прогнан в headless Chromium на странице-примере: места, селекторы из JSON, блокировка на «переводе», обновление при прокрутке, обрезка невидимой части.
- Код на Compose и Android проверен компиляцией против заглушек API: в среде, где он писался, не было доступа к Google Maven, поэтому настоящей сборки ещё не было. Если первая сборка в Android Studio выдаст ошибку, пришлите её текст — это быстро чинится.

### Если Gradle не собирается

- `Unsupported class file major version …` — Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK: выберите JDK 17 или 21.
- Studio предлагает обновить Android Gradle Plugin — можно согласиться или отказаться, на код это не влияет.
- Версии проекта: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01, minSdk 24, compileSdk 35.
