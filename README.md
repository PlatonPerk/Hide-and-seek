# Нычка — прятки в СберKids

Прототип для хакатона «Сбер — Создаем эмоции», трек A «Приколы».

Раз в день персонаж Нычка прячется в одном из разрешённых мест интерфейса. Ребёнок находит его тапом.
Место выбирается по весам из контент-конфига и в один день одинаково у всех.

## Что уже есть

- Нативный Android-прототип на Kotlin + Jetpack Compose (тот же стек и версии, что в песочнице организаторов).
- Вкладки «Главный», «Дела», «Контент», сверстанные по макетам Figma, и плавающий таб-бар с лапкой.
- Слой мест для пряток. Элементы экранов размечены модификаторами `Modifier.hideSpot(id)` (сюда прятаться можно)
  и `Modifier.noHideZone(id)` (сюда нельзя: кнопки оплаты и переводов, баланс, операции, реклама).
  Координаты мест обновляются при скролле. Таб-бар помечен как `occluder`: места, уехавшие под него,
  из выбора исключаются (`HideSpotRegistry.visibleAllowed()`).
- Отладочный режим: **долгое нажатие на лапку** в таб-баре показывает все места на экране.
  Зелёные рамки — разрешённые места, серые — места под таб-баром, красные — запретные зоны.

## Как запустить

1. Открыть папку проекта в Android Studio (JDK 21).
2. Запустить конфигурацию `app` на телефоне (Android 8+) или в эмуляторе.

Готовый APK собирается автоматически на каждый пуш в `main`. Он лежит в ветке
[`ci-results`](../../tree/ci-results) (`nychka-debug.apk`) и в артефактах GitHub Actions.
Там же лежат рендеры экранов (`01_main.png`, …) и лог сборки.

Рендер экранов локально, без телефона:

```bash
./gradlew :app:recordRoborazziDebug   # картинки → app/build/outputs/roborazzi/
```

## Структура

```
app/src/main/java/ru/hideandseek/nychka/
  AppRoot.kt                 корень: вкладки, реестр мест, отладочный слой
  hidespots/HideSpots.kt     hideSpot / noHideZone, HideSpotRegistry, отладочный оверлей
  ui/screens/                MainScreen, TasksScreen, ContentScreen
  ui/components/             таб-бар, кнопки, иконки
  ui/theme/Theme.kt          цвета и типографика из макета
app/src/main/res/drawable-hdpi/   иллюстрации, вырезанные из макета
docs/mockups/                     склеенные макеты экранов (референс)
```

## Как это встраивается в настоящий СберKids

```kotlin
// 1. Повесить якоря на существующие элементы экрана
Avatar(modifier = Modifier.hideSpot("main.avatar"))
QuickActions(modifier = Modifier.noHideZone("main.action_transfer"))

// 2. Обернуть экран: реестр мест + слой Нычки поверх
CompositionLocalProvider(LocalHideSpotRegistry provides registry) {
    Box {
        MainScreen()
        NychkaLayer(registry)   // следующий шаг
    }
}
```

На iOS тот же приём делается через `anchorPreference` / `GeometryReader` в SwiftUI.

## Иллюстрации

Картинки вырезаны из скриншотов Figma в плотности hdpi (~1,5 px на dp). На телефоне они немного мягче оригинала.
Чтобы стало чётче, экспортируйте их из Figma в @3x, положите в `res/drawable-xxhdpi/` с теми же именами
и удалите файлы из `drawable-hdpi/`.
