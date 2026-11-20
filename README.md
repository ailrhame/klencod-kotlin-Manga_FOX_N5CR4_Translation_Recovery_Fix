# Manga FOX — @N5CR4

مشروع Android مكتوب بـ Kotlin لترجمة صفحات المانجا والمانهوا، مع OCR ومعالجة محلية وإدارة نصوص الفقاعات وملفات الصفحات.

## محركات الترجمة

- **OCR محلي متعدد المحركات:** يفحص اللغة من صورة مصغّرة، ثم يشغّل محركات ML Kit ذات القرائن الفعلية معاً عند اكتشاف صفحة مختلطة، ويحتفظ أيضاً بالنصوص القصيرة ذات الحرف الواحد. يحدد لغة النصوص المكتوبة بالأحرف اللاتينية قبل استخدام مترجم ML Kit المناسب بدلاً من افتراض أنها إنجليزية.
- **Qwen محلي:** محرك GGUF يعمل على الجهاز عبر llama.cpp. يتضمن أوضاعاً سريعة ومتوازنة ودقيقة، ويعود التطبيق إلى مسار ML Kit عند تعذر تشغيله.
- **API سحابي متعدد المزودين:** يدعم OpenAI وDeepSeek، إضافة إلى وضع مخصص لأي مزود متوافق مع OpenAI Chat Completions عبر Base URL ومعرّف نموذج يحددهما المستخدم.
- **ذاكرة المصطلحات والشخصيات:** تُستخدم لتوحيد الأسماء والألقاب ونبرة الشخصيات، مع إعطاء الأولوية للمدخلات المرتبطة بنص الصفحة.
- **السياق المتتابع:** تُمرّر آخر الفقاعات وترجماتها العربية إلى الدفعة التالية، كما تُحفظ عينة من سياق الصفحة السابقة.

## إعداد الترجمة السحابية

1. افتح إعدادات التطبيق ثم إعدادات اللغة والترجمة.
2. اختر OpenAI أو DeepSeek، أو اختر **مخصص** لمزود آخر.
3. في الوضع المخصص أدخل Base URL بصيغة HTTPS، ثم معرّف النموذج كما يظهر لدى مزود الخدمة.
4. اختر **إعداد مفتاح API أو استبداله** وأدخل مفتاح المزود المحدد داخل التطبيق.
5. استخدم **اختبار اتصال API المحفوظ** للتحقق من المفتاح والنموذج وعنوان الاتصال، ثم فعّل خيار استخدام API السحابي أولاً.

الوضع المخصص يدعم واجهات Chat Completions المتوافقة مع OpenAI ويستخدم مصادقة Bearer. لا يعني ذلك دعماً تلقائياً لكل بروتوكول خاص أو endpoint مختلف. تُخزّن مفاتيح كل مزود بصورة مستقلة ومشفرة باستخدام Android Keystore وAES/GCM. لا تضف أي مفتاح إلى ملفات المشروع أو Git أو سجلات البناء.

**الخصوصية والتكلفة:** عند تشغيل المحرك السحابي، يرسل التطبيق نصوص OCR المستخرجة، وقاموس المصطلحات، وبيانات الشخصيات إلى المزود الذي تختاره. لا يرسل صورة الصفحة نفسها في هذا المسار. قد تُحتسب رسوم وفق استخدام حساب API؛ اترك الخيار السحابي مغلقاً عند عدم الحاجة إليه. اختبار الاتصال يرسل طلب ترجمة نصياً قصيراً إلى النموذج المختار، لذلك يتحقق من المفتاح والنموذج ونقطة النهاية وصيغة الطلب معاً، وقد يستهلك قدراً صغيراً من الرصيد.

## البناء

المتطلبات المستخدمة في إعداد المشروع:

- JDK 17
- Android SDK Platform 34 وBuild Tools 34.0.0
- Gradle Wrapper 8.14
- Android NDK/CMake حسب إعدادات البيئة المستعملة لبناء المكتبة الأصلية

لبناء نسخة Debug محلياً:

```bash
./gradlew --no-daemon :app:assembleDebug -x lint -x test
```

يوجد سير عمل GitHub Actions في `.github/workflows/build-android.yml` يمكن تشغيله يدوياً من تبويب Actions بعد رفع المشروع إلى مستودعك.

## ملفات أساسية

- `app/src/main/java/com/manga/fox/MainActivity.kt` — واجهة التطبيق ومسار OCR والترجمة والتخزين.
- `app/src/main/java/com/manga/fox/MangaFoxLocalLlm.kt` — تحميل Qwen المحلي وترجمة السياق.
- `app/src/main/AndroidManifest.xml` — صلاحيات التطبيق وخدمة العدسة.
- `app/build.gradle` — إعدادات Android والمكتبات.

## واجهة Dark Studio الجديدة

- رأس تطبيق ثابت يحمل شعار Manga FOX واختصاراً مباشراً للإعدادات.
- شريط تنقل سفلي: الرئيسية، الترجمة، مكتبتي، الاستوديو، والمزيد.
- لوحة رئيسية تعرض إحصائيات المكتبة، آخر الفصول، الاختصارات، وحالة الأذونات.
- تظل صفحات الجودة والمعجم والأرشيف والثيمات والإعدادات متاحة من قائمة «المزيد».
- الثيم الافتراضي أسود/بنفسجي، مع استمرار دعم الثيمات المدمجة والمستوردة.
- استوديو العدسة يتيح 12 لوناً، معاينة مباشرة، تخصيص الرمز والحجم والشفافية، والاختيار بين اللون وصورة الخلفية.
- أيقونة التطبيق تستخدم رسماً متجهياً لقناع الثعلب متناسقاً مع اللون البنفسجي.

## سجل التعديلات

راجع [CHANGELOG_N5CR4.md](CHANGELOG_N5CR4.md) لتفاصيل تحسينات الترجمة الحالية.

---

**تطوير وحقوق المشروع: @N5CR4**


## N5CR4 Advanced additions 16–20

The project source includes the following integrated workflows:

16. **Self-correction / missing-text recovery** — the page translation pipeline retries suspicious or blank results per block, attempts an enhanced OCR read of the affected crop, and keeps failed blocks separate rather than forcing a full-page translation retry. Existing missed-candidate detection remains part of the OCR pipeline.
17. **Character memory and chapter index** — character cards now accept source-language aliases. The translator receives those aliases with voice/notes, and a local bounded index records which saved names/aliases appear in each work/chapter. The index remains on-device.
18. **Bubble and layer editor** — PHANTOM STUDIO exposes the existing page/layer editing workflow: manual region selection, speech/narration layers, erase, direction, position, and undo/redo. Edits remain separate layers; source images are preserved.
19. **Per-page quality report** — the quality guard reports detected layers, estimated translated layers, remaining/empty items, coverage percentage, overlaps, clipping, and suspicious untranslated residue. This is a structural estimate, not semantic proof.
20. **Adaptive translation policy** — a local classifier labels segments as dialogue, narration, sound effect, short text, or mixed-script. Its instruction is passed with each cloud context block and into individual cloud rescue requests. It does not upload images and does not override the source text.

Compatibility: existing character records without an `aliases` field remain readable. New alias data is included in project export/import.

## إضافات الالتقاط الذكي والتحكم بالأزرار

- اختيار اتجاه التقاط الفصل: عمودي من أعلى إلى أسفل، أو أفقي من اليمين إلى اليسار، أو أفقي من اليسار إلى اليمين. يحفظ الاختيار ويستخدمه مسار التمرير ومطابقة التداخل وقص الحافة المتكررة.
- أثناء الالتقاط التلقائي لفصل كامل: زر خفض الصوت يوقف الالتقاط مؤقتاً مع الاحتفاظ باللقطات المكتملة، وزر رفع الصوت يستأنف من الموضع الحالي. خارج الالتقاط تبقى أزرار الصوت للتحكم المعتاد. تتطلب هذه الوظيفة تفعيل صلاحية تصفية أحداث المفاتيح لخدمة إمكانية الوصول.
- استبعاد روابط المواقع والبريد الإلكتروني وعلامات الناشر المائية وعبارات حقوق النشر المعروفة من قائمة OCR المرشحة للترجمة. لا تُزال العلامة من الصورة الأصلية؛ فقط لا تُرسل إلى المترجم ولا تُرسم فوقها ترجمة.
