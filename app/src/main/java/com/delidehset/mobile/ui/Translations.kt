package com.delidehset.mobile.ui

/**
 * Every line of this app, in every language it is written in.
 *
 * The screens are written in Turkish, and this is the dictionary that turns that Turkish
 * into one of the site's eight languages (see `t` / `tf` in AppLang.kt). One [Phrase] is one
 * line: the Turkish it is written as, and what it says in the other seven. All eight are
 * required by the constructor, so a language that was forgotten is a compile error rather
 * than a line that quietly stays Turkish — which is exactly how most of the app ended up
 * Turkish-only in the first place.
 *
 * Lines with a value in them carry `{0}`, `{1}` … and are filled in by `tf`.
 */
internal class Phrase(
    val tr: String,
    val en: String,
    val de: String,
    val es: String,
    val fr: String,
    val pt: String,
    val ru: String,
    val ja: String,
) {
    fun inLang(lang: AppLang): String = when (lang) {
        AppLang.TR -> tr
        AppLang.EN -> en
        AppLang.DE -> de
        AppLang.ES -> es
        AppLang.FR -> fr
        AppLang.PT_BR -> pt
        AppLang.RU -> ru
        AppLang.JA -> ja
    }
}

/** The line, in the language the user picked. Unknown text — a name, a title — is left alone. */
fun translate(lang: AppLang, text: String): String {
    if (lang == AppLang.TR) return text
    return byLang.getValue(lang)[text] ?: text
}

internal val phrases: List<Phrase> = listOf(
    /* ------------------------------------------------ navigation and the shell */
    Phrase("Feed", "Feed", "Feed", "Feed", "Flux", "Feed", "Лента", "フィード"),
    Phrase("Keşfet", "Discover", "Entdecken", "Descubrir", "Découvrir", "Descobrir", "Обзор", "発見"),
    Phrase("Akış & İçerik", "Feed & content", "Feed & Inhalte", "Feed y contenido", "Flux et contenu", "Feed e conteúdo", "Лента и контент", "フィードとコンテンツ"),
    Phrase("Topluluk & İstatistik", "Community & stats", "Community & Statistik", "Comunidad y estadísticas", "Communauté et stats", "Comunidade e estatísticas", "Сообщество и статистика", "コミュニティと統計"),
    Phrase("Yönetim & Destek", "Admin & support", "Verwaltung & Support", "Admin y soporte", "Admin et support", "Admin e suporte", "Админ и поддержка", "管理とサポート"),
    Phrase("Sohbet", "Chat", "Chat", "Chat", "Chat", "Chat", "Чат", "チャット"),
    Phrase("Profil", "Profile", "Profil", "Perfil", "Profil", "Perfil", "Профиль", "プロフィール"),
    Phrase("profil", "profile", "Profil", "perfil", "profil", "perfil", "профиль", "プロフィール"),
    Phrase("Kullanıcı", "User", "Benutzer", "Usuario", "Utilisateur", "Usuário", "Пользователь", "ユーザー"),
    Phrase("Giriş", "Sign in", "Anmelden", "Iniciar sesión", "Connexion", "Entrar", "Войти", "ログイン"),
    Phrase("Oturum aç", "Sign in", "Anmelden", "Iniciar sesión", "Connexion", "Entrar", "Войти", "ログイン"),
    Phrase("Oturum Aç", "Sign in", "Anmelden", "Iniciar sesión", "Connexion", "Entrar", "Войти", "ログイン"),
    Phrase("Giriş yap", "Sign in", "Anmelden", "Iniciar sesión", "Connexion", "Entrar", "Войти", "ログイン"),
    Phrase("Kaydol", "Sign up", "Registrieren", "Registrarse", "S'inscrire", "Cadastrar", "Регистрация", "新規登録"),
    Phrase("Kayıt ol", "Sign up", "Registrieren", "Registrarse", "S'inscrire", "Cadastrar", "Регистрация", "新規登録"),
    Phrase("Çıkış yap", "Sign out", "Abmelden", "Cerrar sesión", "Déconnexion", "Sair", "Выйти", "ログアウト"),
    Phrase("Çıkış Yap", "Sign out", "Abmelden", "Cerrar sesión", "Déconnexion", "Sair", "Выйти", "ログアウト"),
    Phrase("Geri", "Back", "Zurück", "Atrás", "Retour", "Voltar", "Назад", "戻る"),
    Phrase("Menü", "Menu", "Menü", "Menú", "Menu", "Menu", "Меню", "メニュー"),
    Phrase("Ara", "Search", "Suche", "Buscar", "Rechercher", "Buscar", "Поиск", "検索"),
    Phrase("Ayarlar", "Settings", "Einstellungen", "Ajustes", "Paramètres", "Configurações", "Настройки", "設定"),
    Phrase("Kapat", "Close", "Schließen", "Cerrar", "Fermer", "Fechar", "Закрыть", "閉じる"),
    Phrase("Vazgeç", "Cancel", "Abbrechen", "Cancelar", "Annuler", "Cancelar", "Отмена", "キャンセル"),
    Phrase("İptal", "Cancel", "Abbrechen", "Cancelar", "Annuler", "Cancelar", "Отмена", "キャンセル"),
    Phrase("Dil", "Language", "Sprache", "Idioma", "Langue", "Idioma", "Язык", "言語"),
    Phrase("Tekrar dene", "Try again", "Erneut versuchen", "Reintentar", "Réessayer", "Tentar de novo", "Повторить", "再試行"),

    /* ------------------------------------------------ the drawer */
    Phrase("Home", "Home", "Start", "Inicio", "Accueil", "Início", "Главная", "ホーム"),
    Phrase("Topluluklar", "Communities", "Communities", "Comunidades", "Communautés", "Comunidades", "Сообщества", "コミュニティ"),
    Phrase("Topluluk", "Community", "Community", "Comunidad", "Communauté", "Comunidade", "Сообщество", "コミュニティ"),
    Phrase("Effortpostlar", "Effortposts", "Effortposts", "Effortposts", "Effortposts", "Effortposts", "Эффортпосты", "エフォートポスト"),
    Phrase("Gönderi oluştur", "Create post", "Beitrag erstellen", "Crear publicación", "Créer une publication", "Criar publicação", "Создать пост", "投稿を作成"),
    Phrase("Kullanıcılar", "Users", "Benutzer", "Usuarios", "Utilisateurs", "Usuários", "Пользователи", "ユーザー"),
    Phrase("Kayıtlı kullanıcılar", "Registered users", "Registrierte Benutzer", "Usuarios registrados", "Utilisateurs inscrits", "Usuários cadastrados", "Зарегистрированные пользователи", "登録ユーザー"),
    Phrase("Rozetler", "Badges", "Abzeichen", "Insignias", "Badges", "Medalhas", "Значки", "バッジ"),
    Phrase("Leaderboard", "Leaderboard", "Rangliste", "Clasificación", "Classement", "Ranking", "Таблица лидеров", "リーダーボード"),
    Phrase("İstatistikler", "Stats", "Statistik", "Estadísticas", "Statistiques", "Estatísticas", "Статистика", "統計"),
    Phrase("Rastgele gönderi", "Random post", "Zufälliger Beitrag", "Publicación aleatoria", "Publication aléatoire", "Publicação aleatória", "Случайный пост", "ランダム投稿"),
    Phrase("Rastgele kullanıcı", "Random user", "Zufälliger Benutzer", "Usuario aleatorio", "Utilisateur aléatoire", "Usuário aleatório", "Случайный пользователь", "ランダムユーザー"),
    Phrase("Rastgele", "Random", "Zufällig", "Aleatorio", "Aléatoire", "Aleatório", "Случайно", "ランダム"),
    Phrase("Yönetim paneli", "Admin panel", "Adminbereich", "Panel de admin", "Panneau admin", "Painel admin", "Админ-панель", "管理パネル"),
    Phrase("Yöneticiler", "Admins", "Admins", "Admins", "Admins", "Admins", "Админы", "管理者"),
    Phrase("Moderasyon kaydı", "Moderation log", "Moderationsprotokoll", "Registro de moderación", "Journal de modération", "Registro de moderação", "Журнал модерации", "モデレーション履歴"),
    Phrase("Bize ulaşın", "Contact us", "Kontakt", "Contáctanos", "Nous contacter", "Fale conosco", "Связаться с нами", "お問い合わせ"),
    Phrase("Gönderi", "Post", "Beitrag", "Publicación", "Publication", "Publicação", "Пост", "投稿"),
    Phrase("Gönderiler", "Posts", "Beiträge", "Publicaciones", "Publications", "Publicações", "Посты", "投稿"),
    Phrase("Yorumlar", "Comments", "Kommentare", "Comentarios", "Commentaires", "Comentários", "Комментарии", "コメント"),
    Phrase("Yanıtla", "Reply", "Antworten", "Responder", "Répondre", "Responder", "Ответить", "返信"),
    Phrase("Paylaş", "Share", "Teilen", "Compartir", "Partager", "Compartilhar", "Поделиться", "共有"),
    Phrase("Gönder", "Send", "Senden", "Enviar", "Envoyer", "Enviar", "Отправить", "送信"),
    Phrase("Gönderiliyor…", "Sending…", "Senden…", "Enviando…", "Envoi…", "Enviando…", "Отправка…", "送信中…"),
    Phrase("Sil", "Delete", "Löschen", "Eliminar", "Supprimer", "Excluir", "Удалить", "削除"),
    Phrase("Şikâyet", "Report", "Melden", "Denunciar", "Signaler", "Denunciar", "Жалоба", "通報"),
    Phrase("Gönderiyi şikâyet et", "Report post", "Beitrag melden", "Denunciar publicación", "Signaler la publication", "Denunciar publicação", "Пожаловаться на пост", "投稿を報告"),
    Phrase("Sebep", "Reason", "Grund", "Motivo", "Raison", "Motivo", "Причина", "理由"),
    Phrase("kural ihlali", "rule violation", "Regelverstoß", "infracción de normas", "infraction aux règles", "violação de regras", "нарушение правил", "ルール違反"),
    Phrase("Zorunlu", "Required", "Pflichtfeld", "Obligatorio", "Obligatoire", "Obrigatório", "Обязательно", "必須"),
    Phrase("Metin", "Text", "Text", "Texto", "Texte", "Texto", "Текст", "テキスト"),
    Phrase("Henüz gönderi yok.", "No posts yet.", "Noch keine Beiträge.", "Aún no hay publicaciones.", "Pas encore de publications.", "Ainda não há publicações.", "Постов пока нет.", "まだ投稿がありません。"),
    Phrase("Henüz topluluk yok.", "No communities yet.", "Noch keine Communitys.", "Aún no hay comunidades.", "Pas encore de communautés.", "Ainda não há comunidades.", "Сообществ пока нет.", "まだコミュニティがありません。"),
    Phrase("Kullanıcı yok.", "No users.", "Keine Benutzer.", "No hay usuarios.", "Aucun utilisateur.", "Nenhum usuário.", "Пользователей нет.", "ユーザーがいません。"),
    Phrase("Rozet bulunamadı.", "Badge not found.", "Abzeichen nicht gefunden.", "Insignia no encontrada.", "Badge introuvable.", "Medalha não encontrada.", "Значок не найден.", "バッジが見つかりません。"),
    Phrase("Topluluk bulunamadı", "Community not found", "Community nicht gefunden", "Comunidad no encontrada", "Communauté introuvable", "Comunidade não encontrada", "Сообщество не найдено", "コミュニティが見つかりません"),
    Phrase("Bu sayfada listelenecek bir şey yok.", "Nothing to list on this page.", "Auf dieser Seite gibt es nichts aufzulisten.", "No hay nada que listar en esta página.", "Rien à afficher sur cette page.", "Nada para listar nesta página.", "На этой странице нечего показывать.", "このページに表示するものはありません。"),
    Phrase("Sonuç yok.", "No results.", "Keine Ergebnisse.", "Sin resultados.", "Aucun résultat.", "Sem resultados.", "Нет результатов.", "結果なし。"),
    Phrase("Aramak için bir şey yaz.", "Type something to search.", "Gib etwas zum Suchen ein.", "Escribe algo para buscar.", "Écris quelque chose à rechercher.", "Digite algo para buscar.", "Введите что-нибудь для поиска.", "検索する内容を入力してください。"),
    Phrase("Ara (ör. 'hole:general')", "Search (e.g. 'hole:general')", "Suche (z. B. 'hole:general')", "Buscar (p. ej. 'hole:general')", "Rechercher (ex. « hole:general »)", "Buscar (ex. 'hole:general')", "Поиск (напр. 'hole:general')", "検索（例: 'hole:general'）"),

    /* ------------------------------------------------ account settings */
    Phrase("Ayarlar okunamadı. Boş değerlerle hesabının üzerine yazmamak için kaydedilmedi.", "The settings could not be read. Nothing was saved, so empty values would not overwrite your account.", "Die Einstellungen konnten nicht gelesen werden. Es wurde nichts gespeichert, damit leere Werte dein Konto nicht überschreiben.", "No se pudieron leer los ajustes. No se guardó nada para no sobrescribir tu cuenta con valores vacíos.", "Les réglages n'ont pas pu être lus. Rien n'a été enregistré pour ne pas écraser ton compte avec des valeurs vides.", "Não foi possível ler as configurações. Nada foi salvo para não sobrescrever sua conta com valores vazios.", "Не удалось прочитать настройки. Ничего не сохранено, чтобы пустые значения не перезаписали аккаунт.", "設定を読み込めませんでした。空の値でアカウントを上書きしないため、保存していません。"),
    Phrase("Hesap Ayarları", "Account settings", "Konto-Einstellungen", "Ajustes de cuenta", "Paramètres du compte", "Configurações da conta", "Настройки аккаунта", "アカウント設定"),
    Phrase("Kişisel Ayarlar", "Personal settings", "Persönliche Einstellungen", "Ajustes personales", "Paramètres personnels", "Configurações pessoais", "Личные настройки", "個人設定"),
    Phrase("Güvenlik ve Şifre", "Security & password", "Sicherheit & Passwort", "Seguridad y contraseña", "Sécurité et mot de passe", "Segurança e senha", "Безопасность и пароль", "セキュリティとパスワード"),
    Phrase("Oturum", "Session", "Sitzung", "Sesión", "Session", "Sessão", "Сеанс", "セッション"),
    Phrase("Kişisel", "Personal", "Persönlich", "Personal", "Personnel", "Pessoal", "Личное", "個人"),
    Phrase("Güvenlik", "Security", "Sicherheit", "Seguridad", "Sécurité", "Segurança", "Безопасность", "セキュリティ"),
    Phrase("Tema", "Theme", "Design", "Tema", "Thème", "Tema", "Тема", "テーマ"),
    Phrase("Tema rengi", "Accent colour", "Akzentfarbe", "Color de acento", "Couleur d'accent", "Cor de destaque", "Цвет акцента", "アクセントカラー"),
    Phrase("Uygulama dili", "App language", "App-Sprache", "Idioma de la app", "Langue de l'app", "Idioma do app", "Язык приложения", "アプリの言語"),
    Phrase("Site dili", "Site language", "Website-Sprache", "Idioma del sitio", "Langue du site", "Idioma do site", "Язык сайта", "サイトの言語"),
    Phrase("Uygulamanın kendi dili; site dili ayrı kalır.", "The app's own language; the site's stays separate.", "Die Sprache der App; die der Website bleibt separat.", "El idioma de la app; el del sitio queda aparte.", "La langue de l'app ; celle du site reste séparée.", "O idioma do app; o do site fica separado.", "Язык приложения; язык сайта остаётся отдельным.", "アプリの言語です。サイトの言語は別です。"),
    Phrase("Uygulama teması sitenin temasından bağımsızdır.", "The app's theme is independent of the site's.", "Das App-Design ist unabhängig von dem der Website.", "El tema de la app es independiente del del sitio.", "Le thème de l'app est indépendant de celui du site.", "O tema do app é independente do site.", "Тема приложения не зависит от темы сайта.", "アプリのテーマはサイトのテーマとは別です。"),
    Phrase("Profil fotoğrafı", "Profile picture", "Profilbild", "Foto de perfil", "Photo de profil", "Foto de perfil", "Фото профиля", "プロフィール画像"),
    Phrase("Profili Düzenle", "Edit profile", "Profil bearbeiten", "Editar perfil", "Modifier le profil", "Editar perfil", "Редактировать профиль", "プロフィールを編集"),
    Phrase("Profil resmi, biyografi ve tercihler", "Profile picture, bio and preferences", "Profilbild, Bio und Einstellungen", "Foto de perfil, biografía y preferencias", "Photo de profil, bio et préférences", "Foto de perfil, bio e preferências", "Фото профиля, описание и настройки", "プロフィール画像・自己紹介・設定"),
    Phrase("Parola güncelleme ve oturum güvenliği", "Password updates and session security", "Passwortänderung und Sitzungssicherheit", "Cambio de contraseña y seguridad de la sesión", "Changement de mot de passe et sécurité de session", "Troca de senha e segurança da sessão", "Смена пароля и безопасность сеанса", "パスワード変更とセッションの安全性"),
    Phrase("Profil yüklenemedi.", "The profile could not be loaded.", "Das Profil konnte nicht geladen werden.", "No se pudo cargar el perfil.", "Impossible de charger le profil.", "Não foi possível carregar o perfil.", "Не удалось загрузить профиль.", "プロフィールを読み込めませんでした。"),
    Phrase("Bu profil henüz ek bir ayrıntı paylaşmamış.", "This profile hasn't shared any extra details yet.", "Dieses Profil hat noch keine weiteren Angaben geteilt.", "Este perfil aún no ha compartido más detalles.", "Ce profil n'a pas encore partagé de détails.", "Este perfil ainda não compartilhou detalhes extras.", "Этот профиль пока не добавил дополнительных сведений.", "このプロフィールにはまだ詳細がありません。"),
    Phrase("Gönderi aktivitesi", "Post activity", "Beitragsaktivität", "Actividad de publicaciones", "Activité de publication", "Atividade de publicações", "Активность постов", "投稿アクティビティ"),
    Phrase("Mevcut oturumu sonlandır", "End the current session", "Sitzung beenden", "Cerrar la sesión actual", "Terminer la session", "Encerrar a sessão atual", "Завершить сеанс", "現在のセッションを終了"),
    Phrase("Hesabınızdan çıkış yapmak istediğinize emin misiniz?", "Are you sure you want to sign out of your account?", "Möchtest du dich wirklich abmelden?", "¿Seguro que quieres cerrar sesión?", "Voulez-vous vraiment vous déconnecter ?", "Tem certeza de que quer sair da conta?", "Вы уверены, что хотите выйти из аккаунта?", "本当にログアウトしますか？"),
    Phrase("Gönderi silinsin mi?", "Delete this post?", "Diesen Beitrag löschen?", "¿Eliminar esta publicación?", "Supprimer cette publication ?", "Excluir esta publicação?", "Удалить этот пост?", "この投稿を削除しますか？"),
    Phrase("Değişiklikleri kaydet", "Save changes", "Änderungen speichern", "Guardar cambios", "Enregistrer les changements", "Salvar alterações", "Сохранить изменения", "変更を保存"),
    Phrase("Değişiklik yok.", "Nothing changed.", "Keine Änderung.", "Sin cambios.", "Aucun changement.", "Nada mudou.", "Изменений нет.", "変更はありません。"),
    Phrase("Kaydedildi.", "Saved.", "Gespeichert.", "Guardado.", "Enregistré.", "Salvo.", "Сохранено.", "保存しました。"),
    Phrase("Hakkında (bio)", "About (bio)", "Über (Bio)", "Acerca de (bio)", "À propos (bio)", "Sobre (bio)", "О себе (bio)", "自己紹介（bio）"),
    Phrase("Profilinde görünür. En fazla 500 karakter.", "Shown on your profile. Max 500 characters.", "Erscheint in deinem Profil. Max. 500 Zeichen.", "Se muestra en tu perfil. Máx. 500 caracteres.", "Visible sur ton profil. 500 caractères max.", "Aparece no seu perfil. Máx. 500 caracteres.", "Отображается в профиле. Не более 500 символов.", "プロフィールに表示されます。最大500文字。"),
    Phrase("Zamirler", "Pronouns", "Pronomen", "Pronombres", "Pronoms", "Pronomes", "Местоимения", "代名詞"),
    Phrase("En fazla 15 karakter, örn. o/onun.", "Max 15 characters, e.g. they/them.", "Max. 15 Zeichen, z. B. er/ihm.", "Máx. 15 caracteres, p. ej. él.", "15 caractères max, ex. il/lui.", "Máx. 15 caracteres, ex. ele/dele.", "Не более 15 символов, напр. он/его.", "最大15文字（例: 彼/彼の）"),
    Phrase("Fotoğraf seç", "Choose a photo", "Foto wählen", "Elegir foto", "Choisir une photo", "Escolher foto", "Выбрать фото", "写真を選択"),
    Phrase("Banner seç", "Choose a banner", "Banner wählen", "Elegir banner", "Choisir une bannière", "Escolher banner", "Выбрать баннер", "バナーを選択"),
    Phrase("Görsel seçtiğin an yüklenir. En fazla 8 MB.", "Uploads as soon as you pick it. Max 8 MB.", "Wird sofort nach der Auswahl hochgeladen. Max 8 MB.", "Se sube al seleccionarlo. Máx. 8 MB.", "Envoyé dès la sélection. Max 8 Mo.", "Envia assim que escolher. Máx. 8 MB.", "Загрузится сразу после выбора. Макс. 8 МБ.", "選んだ瞬間にアップロードされます。最大8MB。"),
    Phrase("Yüklemeler siteye doğrudan gider; düz tarayıcı açılmaz.", "Uploads go straight to the site; no plain browser opens.", "Uploads gehen direkt an die Website; es öffnet sich kein nackter Browser.", "Las subidas van directas al sitio; no se abre un navegador aparte.", "Les envois vont directement au site ; aucun navigateur ne s'ouvre.", "Os envios vão direto para o site; nenhum navegador abre.", "Загрузки идут прямо на сайт; отдельный браузер не открывается.", "アップロードは直接サイトへ送られます。ブラウザは開きません。"),
    Phrase("Görsel okunamadı. Başka bir dosya seç.", "The image could not be read. Pick another file.", "Das Bild konnte nicht gelesen werden. Wähle eine andere Datei.", "No se pudo leer la imagen. Elige otro archivo.", "Impossible de lire l'image. Choisis un autre fichier.", "Não foi possível ler a imagem. Escolha outro arquivo.", "Не удалось прочитать изображение. Выберите другой файл.", "画像を読み込めませんでした。別のファイルを選んでください。"),
    Phrase("Kişisel sekmedeki değişiklikler tek seferde siteye gönderilir.", "Changes on the Personal tab are sent to the site in one go.", "Änderungen im persönlichen Tab werden gesammelt gesendet.", "Los cambios de la pestaña Personal se envían al sitio de una vez.", "Les changements de l'onglet Personnel sont envoyés au site en une fois.", "As mudanças da aba Pessoal são enviadas ao site de uma vez.", "Изменения на вкладке «Личное» отправляются на сайт сразу.", "「個人」タブの変更はまとめてサイトに送信されます。"),
    Phrase("Mevcut ve yeni şifre gerekli.", "The current and the new password are both required.", "Das aktuelle und das neue Passwort sind beide nötig.", "Se requieren la contraseña actual y la nueva.", "Le mot de passe actuel et le nouveau sont requis.", "A senha atual e a nova são obrigatórias.", "Нужны текущий и новый пароль.", "現在のパスワードと新しいパスワードが必要です。"),
    Phrase("Yeni şifre en az 8 karakter olmalı.", "The new password must be at least 8 characters.", "Das neue Passwort muss mindestens 8 Zeichen haben.", "La nueva contraseña debe tener al menos 8 caracteres.", "Le nouveau mot de passe doit faire au moins 8 caractères.", "A nova senha precisa ter pelo menos 8 caracteres.", "Новый пароль должен быть не короче 8 символов.", "新しいパスワードは8文字以上必要です。"),
    Phrase("Yeni şifreler eşleşmiyor.", "The new passwords do not match.", "Die neuen Passwörter stimmen nicht überein.", "Las nuevas contraseñas no coinciden.", "Les nouveaux mots de passe ne correspondent pas.", "As novas senhas não coincidem.", "Новые пароли не совпадают.", "新しいパスワードが一致しません。"),
    Phrase("Onay için mevcut şifreni girmen gerekir.", "Current password is required to confirm.", "Zur Bestätigung ist das aktuelle Passwort nötig.", "Se requiere la contraseña actual para confirmar.", "Le mot de passe actuel est requis pour confirmer.", "A senha atual é necessária para confirmar.", "Для подтверждения нужен текущий пароль.", "確認には現在のパスワードが必要です。"),
    Phrase("Şifre değiştir", "Change password", "Passwort ändern", "Cambiar contraseña", "Changer le mot de passe", "Alterar senha", "Изменить пароль", "パスワード変更"),
    Phrase("Şifreyi değiştir", "Change password", "Passwort ändern", "Cambiar contraseña", "Changer le mot de passe", "Alterar senha", "Изменить пароль", "パスワードを変更"),
    Phrase("Yeni şifre", "New password", "Neues Passwort", "Nueva contraseña", "Nouveau mot de passe", "Nova senha", "Новый пароль", "新しいパスワード"),
    Phrase("Yeni şifre (tekrar)", "New password (again)", "Neues Passwort (erneut)", "Nueva contraseña (otra vez)", "Nouveau mot de passe (encore)", "Nova senha (de novo)", "Новый пароль (ещё раз)", "新しいパスワード（再入力）"),
    Phrase("Mevcut şifre", "Current password", "Aktuelles Passwort", "Contraseña actual", "Mot de passe actuel", "Senha atual", "Текущий пароль", "現在のパスワード"),
    Phrase("Şifre", "Password", "Passwort", "Contraseña", "Mot de passe", "Senha", "Пароль", "パスワード"),
    Phrase("Şifre güncellendi.", "Password updated.", "Passwort aktualisiert.", "Contraseña actualizada.", "Mot de passe mis à jour.", "Senha atualizada.", "Пароль обновлён.", "パスワードを更新しました。"),
    Phrase("E-posta adresi", "Email address", "E-Mail-Adresse", "Correo electrónico", "Adresse e-mail", "E-mail", "Эл. почта", "メールアドレス"),
    Phrase("E-postayı güncelle", "Update email", "E-Mail aktualisieren", "Actualizar correo", "Mettre à jour l'e-mail", "Atualizar e-mail", "Обновить почту", "メールを更新"),
    Phrase("Kullanıcı adı", "Username", "Benutzername", "Nombre de usuario", "Nom d'utilisateur", "Nome de usuário", "Имя пользователя", "ユーザー名"),
    Phrase("İki adımlı doğrulama", "Two-factor authentication", "Zwei-Faktor-Authentifizierung", "Verificación en dos pasos", "Authentification à deux facteurs", "Verificação em duas etapas", "Двухфакторная аутентификация", "二段階認証"),
    Phrase("2FA kurulumu QR kod okutmayı gerektirdiği için bu adım şimdilik site üzerinden yapılıyor. Şifre ve e-posta değişikliği uygulama içinde.", "Setting up 2FA needs a QR code to be scanned, so for now this step happens on the site. Password and email changes are done in the app.", "Die 2FA-Einrichtung braucht einen QR-Code zum Scannen, deshalb passiert dieser Schritt vorerst auf der Website. Passwort- und E-Mail-Änderungen laufen in der App.", "Configurar 2FA requiere escanear un código QR, así que por ahora este paso se hace en el sitio. La contraseña y el correo se cambian en la app.", "Configurer la 2FA demande de scanner un QR code, donc pour l'instant cette étape se fait sur le site. Le mot de passe et l'e-mail se changent dans l'app.", "Configurar o 2FA exige escanear um QR code, então por enquanto esse passo é feito no site. Senha e e-mail são alterados no app.", "Для настройки 2FA нужно отсканировать QR-код, поэтому пока этот шаг делается на сайте. Пароль и почта меняются в приложении.", "2FAの設定にはQRコードの読み取りが必要なため、今のところこの手順はサイトで行います。パスワードとメールの変更はアプリ内でできます。"),
    Phrase("Giriş yaptığın an oturum uygulamaya aktarılır, gerisi native devam eder.", "The moment you sign in, the session moves into the app and everything continues natively.", "Sobald du dich anmeldest, wandert die Sitzung in die App und alles läuft nativ weiter.", "Al iniciar sesión, la sesión pasa a la app y todo continúa de forma nativa.", "Dès la connexion, la session passe dans l'app et tout continue en natif.", "Assim que você entrar, a sessão passa para o app e tudo continua nativo.", "Сразу после входа сеанс переходит в приложение, дальше всё работает нативно.", "ログインするとセッションがアプリに移り、以降はネイティブで動作します。"),
    Phrase("Bu hesap için site tarafında 2FA kurulumu görünmüyor.", "No two-factor setup is visible for this account on the site.", "Für dieses Konto ist auf der Website keine 2FA-Einrichtung zu sehen.", "No hay verificación en dos pasos configurada para esta cuenta.", "Aucune configuration 2FA n'apparaît pour ce compte.", "Nenhuma configuração 2FA aparece para esta conta.", "Для этого аккаунта двухфакторная аутентификация не настроена.", "このアカウントには2段階認証の設定が見つかりません。"),

    /* ------------------------------------------------ signing in */
    Phrase("Giriş başarılı.", "Signed in.", "Angemeldet.", "Sesión iniciada.", "Connecté.", "Sessão iniciada.", "Вы вошли.", "ログインしました。"),
    Phrase("Oturum uygulamaya aktarıldı.", "Session moved into the app.", "Sitzung in die App übernommen.", "Sesión transferida a la app.", "Session transférée dans l'app.", "Sessão transferida para o app.", "Сеанс перенесён в приложение.", "セッションをアプリに移しました。"),
    Phrase("Delidehşet hesabınla giriş yap. Oturum cihazda saklanır.", "Sign in with your Delidehşet account. The session is kept on this device.", "Melde dich mit deinem Delidehşet-Konto an. Die Sitzung bleibt auf dem Gerät.", "Inicia sesión con tu cuenta de Delidehşet. La sesión se guarda en el dispositivo.", "Connecte-toi avec ton compte Delidehşet. La session reste sur l'appareil.", "Entre com sua conta Delidehşet. A sessão fica no aparelho.", "Войдите в аккаунт Delidehşet. Сеанс хранится на устройстве.", "Delidehşetアカウントでログイン。セッションは端末に保存されます。"),
    Phrase("Kayıt ol (uygulama içinde)", "Sign up (in the app)", "Registrieren (in der App)", "Registrarse (en la app)", "S'inscrire (dans l'app)", "Cadastrar (no app)", "Регистрация (в приложении)", "新規登録（アプリ内）"),
    Phrase("Kayıt, Cloudflare Turnstile istediği için site sayfası burada açılıyor.", "Signing up needs Cloudflare Turnstile, so the site's page opens here.", "Für die Registrierung verlangt die Seite Cloudflare Turnstile, deshalb öffnet hier die Website.", "El registro requiere Cloudflare Turnstile, por eso se abre aquí la página del sitio.", "L'inscription demande Cloudflare Turnstile, donc la page du site s'ouvre ici.", "O cadastro exige Cloudflare Turnstile, então a página do site abre aqui.", "Для регистрации нужен Cloudflare Turnstile, поэтому здесь открывается страница сайта.", "登録にはCloudflare Turnstileが必要なため、サイトのページをここで開きます。"),
    Phrase("Kayıt ekranı, site doğrulaması için uygulama içinde açılır; giriş yaptığın an oturum native tarafa aktarılır.", "The sign-up screen opens inside the app because the site verifies it there; the moment you sign in, the session moves to the native side.", "Die Registrierung öffnet sich in der App, weil die Website sie dort verifiziert; sobald du angemeldet bist, wandert die Sitzung in den nativen Teil.", "La pantalla de registro se abre dentro de la app porque el sitio la verifica ahí; al iniciar sesión, la sesión pasa al lado nativo.", "L'inscription s'ouvre dans l'app car le site la vérifie là ; dès la connexion, la session passe au natif.", "A tela de cadastro abre dentro do app porque o site a verifica ali; ao entrar, a sessão passa para o lado nativo.", "Экран регистрации открывается внутри приложения, потому что сайт проверяет её там; после входа сеанс переходит в нативную часть.", "登録画面はサイトの検証のためアプリ内で開きます。ログインするとセッションはネイティブ側に移ります。"),
    Phrase("Giriş yapamıyor musun?", "Can't sign in?", "Anmeldung nicht möglich?", "¿No puedes entrar?", "Impossible de te connecter ?", "Não consegue entrar?", "Не можете войти?", "ログインできない？"),
    Phrase("Site içinde dene", "Try it on the site", "Auf der Website versuchen", "Probar en el sitio", "Essayer sur le site", "Tentar no site", "Попробовать на сайте", "サイトで試す"),
    Phrase("Sayfayı uygulamada aç", "Open the page in the app", "Seite in der App öffnen", "Abrir la página en la app", "Ouvrir la page dans l'app", "Abrir a página no app", "Открыть страницу в приложении", "ページをアプリで開く"),

    /* ------------------------------------------------ posting */
    Phrase("Anonim gönder", "Post anonymously", "Anonym posten", "Publicar como anónimo", "Publier anonymement", "Publicar anonimamente", "Анонимный пост", "匿名で投稿"),
    Phrase("Normal (anonim olmayan) gönderi için oturum aç", "Sign in to post normally (not anonymously)", "Melde dich an, um normal (nicht anonym) zu posten", "Inicia sesión para publicar de forma normal (no anónima)", "Connecte-toi pour publier normalement (pas anonymement)", "Entre para publicar normalmente (sem ser anônimo)", "Войдите, чтобы публиковать обычным (не анонимным) постом", "通常（匿名でない）投稿にはログインが必要です"),
    Phrase("Oturum açmadan da gönderi paylaşabilirsin (anonim).", "You can also post without signing in (anonymously).", "Du kannst auch ohne Anmeldung posten (anonym).", "También puedes publicar sin iniciar sesión (anónimo).", "Tu peux aussi publier sans te connecter (anonymement).", "Você também pode publicar sem entrar (anônimo).", "Можно публиковать и без входа (анонимно).", "ログインせずに投稿できます（匿名）。"),
    Phrase("Başlık (en fazla 500)", "Title (max 500)", "Titel (max. 500)", "Título (máx. 500)", "Titre (max 500)", "Título (máx. 500)", "Заголовок (до 500)", "タイトル（最大500）"),
    Phrase("Bir topluluk seç", "Choose a community", "Wähle eine Community", "Elige una comunidad", "Choisis une communauté", "Escolha uma comunidade", "Выберите сообщество", "コミュニティを選択"),
    Phrase("Bağlantı veya görsel varsa isteğe bağlı", "Optional link or image", "Optionaler Link oder Bild", "Enlace o imagen opcional", "Lien ou image facultatif", "Link ou imagem opcional", "Ссылка или картинка (необязательно)", "リンクまたは画像（任意）"),
    Phrase("Site ile aynı alanlar: title, body, hole.", "Same fields as the site: title, body, hole.", "Dieselben Felder wie auf der Website: title, body, hole.", "Los mismos campos que el sitio: title, body, hole.", "Mêmes champs que le site : title, body, hole.", "Os mesmos campos do site: title, body, hole.", "Те же поля, что на сайте: title, body, hole.", "サイトと同じ項目: title, body, hole。"),
    Phrase("Gönderi paylaşıldı.", "Post shared.", "Beitrag geteilt.", "Publicación compartida.", "Publication partagée.", "Publicação compartilhada.", "Пост опубликован.", "投稿を共有しました。"),
    Phrase("Gönderi yüklenemedi.", "The post could not be loaded.", "Der Beitrag konnte nicht geladen werden.", "No se pudo cargar la publicación.", "Impossible de charger la publication.", "Não foi possível carregar a publicação.", "Не удалось загрузить пост.", "投稿を読み込めませんでした。"),
    Phrase("Tripcode gizli değeri", "Tripcode secret value", "Geheimer Tripcode-Wert", "Valor secreto del tripcode", "Valeur secrète du tripcode", "Valor secreto do tripcode", "Секретное значение tripcode", "トリップコードの秘密値"),
    Phrase("Aynı gizli değeri kullan", "Use the same secret value", "Denselben geheimen Wert nutzen", "Usar el mismo valor secreto", "Utiliser la même valeur secrète", "Usar o mesmo valor secreto", "Использовать то же секретное значение", "同じ秘密の値を使う"),
    Phrase("Tripcode ile aynı kimlik altında görünürsün, gizli değer saklanmaz.", "You appear under the same identity with a tripcode; the secret value is not stored.", "Mit einem Tripcode erscheinst du unter derselben Identität; der geheime Wert wird nicht gespeichert.", "Con un tripcode apareces bajo la misma identidad; el valor secreto no se guarda.", "Avec un tripcode tu apparais sous la même identité ; la valeur secrète n'est pas conservée.", "Com um tripcode você aparece sob a mesma identidade; o valor secreto não é guardado.", "С tripcode вы выступаете под одним и тем же именем; секретное значение не сохраняется.", "トリップコードを使うと同じ名前で表示されます。秘密値は保存されません。"),

    /* ------------------------------------------------ the room */
    Phrase("Sesli sohbet", "Voice chat", "Sprachchat", "Chat de voz", "Salon vocal", "Chat de voz", "Голосовой чат", "ボイスチャット"),
    Phrase("Sese katıl", "Join voice", "Sprachchat beitreten", "Unirse a la voz", "Rejoindre la voix", "Entrar na voz", "Войти в голосовой чат", "ボイスに参加"),
    Phrase("Sese katılınca mikrofonun odaya açılır.", "Joining the call opens your microphone to the room.", "Beim Beitritt wird dein Mikrofon für den Raum geöffnet.", "Al unirte, tu micrófono se abre a la sala.", "Rejoindre l'appel ouvre ton micro au salon.", "Ao entrar na chamada, seu microfone abre para a sala.", "При входе микрофон открывается для комнаты.", "通話に参加するとマイクが部屋に開きます。"),
    Phrase("Seste değilsin", "You are not in the call", "Du bist nicht im Call", "No estás en la llamada", "Vous n'êtes pas en appel", "Você não está na chamada", "Вы не в звонке", "通話に参加していません"),
    Phrase("Seste", "In voice", "Im Sprachchat", "En voz", "En vocal", "Em voz", "В голосе", "ボイス参加中"),
    Phrase("Sestesin", "In the call", "Im Call", "En la llamada", "En appel", "Na chamada", "В звонке", "通話中"),
    Phrase("Seste • mikrofon açık", "In voice • mic on", "Im Sprachchat • Mikro an", "En voz • micro activado", "En vocal • micro actif", "Em voz • microfone ativo", "В голосе • микрофон включён", "ボイス参加中 • マイクON"),
    Phrase("Seste • mikrofon kapalı", "In voice • mic off", "Im Sprachchat • Mikro aus", "En voz • micro silenciado", "En vocal • micro coupé", "Em voz • microfone mudo", "В голосе • микрофон выключен", "ボイス参加中 • マイクOFF"),
    Phrase("Sestesin • mikrofon açık", "In the call • mic on", "Im Call • Mikro an", "En la llamada • micro activado", "En appel • micro actif", "Na chamada • microfone ativo", "В звонке • микрофон включён", "通話中 • マイクON"),
    Phrase("Sestesin • mikrofon kapalı", "In the call • mic off", "Im Call • Mikro aus", "En la llamada • micro silenciado", "En appel • micro coupé", "Na chamada • microfone mudo", "В звонке • микрофон выключен", "通話中 • マイクOFF"),
    Phrase("Seste kimse yok — ilk gelen sen ol.", "Nobody is in voice — be the first.", "Niemand ist im Sprachchat — sei die erste Person.", "No hay nadie en voz: sé el primero.", "Personne en vocal — sois le premier.", "Ninguém na voz — seja o primeiro.", "В голосовом чате никого нет — будьте первым.", "ボイスには誰もいません。最初の一人になろう。"),
    Phrase("Mikrofonu aç", "Unmute", "Mikrofon an", "Activar micrófono", "Activer le micro", "Ativar microfone", "Включить микрофон", "ミュート解除"),
    Phrase("Mikrofonu kapat", "Mute", "Mikrofon aus", "Silenciar micrófono", "Couper le micro", "Silenciar microfone", "Выключить микрофон", "ミュート"),
    Phrase("Sağırlaştır", "Deafen", "Taub schalten", "Ensordecer", "Rendre sourd", "Ensurdecer", "Заглушить", "スピーカーをミュート"),
    Phrase("Sesi geri aç", "Un-deafen", "Ton wieder an", "Reactivar sonido", "Réactiver le son", "Reativar som", "Вернуть звук", "スピーカーのミュート解除"),
    Phrase("Kulaklık", "Headphones", "Kopfhörer", "Auriculares", "Casque", "Fones", "Наушники", "ヘッドホン"),
    Phrase("Hoparlör", "Speaker", "Lautsprecher", "Altavoz", "Haut-parleur", "Alto-falante", "Динамик", "スピーカー"),
    Phrase("Sesten ayrıl", "Leave the call", "Call verlassen", "Salir de la llamada", "Quitter l'appel", "Sair da chamada", "Покинуть звонок", "通話から退出"),
    Phrase("Ayrıl", "Leave", "Verlassen", "Salir", "Quitter", "Sair", "Выйти", "退出"),
    Phrase("Mikrofonu kapatmak ve ayrılmak için üstteki çubuğu kullan.", "Use the bar above to mute and leave.", "Nutze die Leiste oben zum Stummschalten und Verlassen.", "Usa la barra de arriba para silenciar y salir.", "Utilise la barre en haut pour couper le micro et quitter.", "Use a barra acima para silenciar e sair.", "Используйте панель сверху, чтобы выключить микрофон и выйти.", "上のバーでミュートと退出ができます。"),
    Phrase("Mikrofon izni verilmedi. Ayarlardan izin verip tekrar dene.", "Microphone permission denied. Allow it in settings and try again.", "Mikrofon-Berechtigung verweigert. Erlaube sie in den Einstellungen.", "Permiso de micrófono denegado. Concédelo en ajustes.", "Permission micro refusée. Autorise-la dans les réglages.", "Permissão de microfone negada. Permita nas configurações.", "Доступ к микрофону запрещён. Разрешите его в настройках.", "マイクの権限がありません。設定で許可してください。"),
    Phrase("Odaya bağlanılıyor…", "Connecting to the room…", "Verbinde mit dem Raum…", "Conectando a la sala…", "Connexion au salon…", "Conectando à sala…", "Подключение к комнате…", "部屋に接続中…"),
    Phrase("Bağlanıyor…", "Connecting…", "Verbinden…", "Conectando…", "Connexion…", "Conectando…", "Подключение…", "接続中…"),
    Phrase("Bağlı", "Connected", "Verbunden", "Conectado", "Connecté", "Conectado", "Подключено", "接続済み"),
    Phrase("Bağlısın", "You're connected", "Du bist verbunden", "Estás conectado", "Tu es connecté", "Você está conectado", "Вы подключены", "接続しています"),
    Phrase("En yeni mesaja in", "Jump to newest", "Zu den neuesten", "Ir a lo más nuevo", "Aller au plus récent", "Ir para o mais novo", "К новым сообщениям", "最新へ移動"),
    Phrase("Oda boş. İlk mesajı sen yaz.", "The room is empty. Write the first message.", "Der Raum ist leer. Schreib die erste Nachricht.", "La sala está vacía. Escribe el primer mensaje.", "Le salon est vide. Écris le premier message.", "A sala está vazia. Escreva a primeira mensagem.", "Комната пуста. Напишите первое сообщение.", "部屋は空です。最初のメッセージを書こう。"),
    Phrase("Mesaj yaz…", "Message…", "Nachricht…", "Mensaje…", "Message…", "Mensagem…", "Сообщение…", "メッセージ…"),
    Phrase("Bir yanıt yaz…", "Write a reply…", "Antwort schreiben…", "Escribe una respuesta…", "Écris une réponse…", "Escreva uma resposta…", "Напишите ответ…", "返信を書く…"),
    Phrase("Mesaj kopyalandı", "Message copied", "Nachricht kopiert", "Mensaje copiado", "Message copié", "Mensagem copiada", "Сообщение скопировано", "メッセージをコピーしました"),
    Phrase("Mesaj gönderildi.", "Message sent.", "Nachricht gesendet.", "Mensaje enviado.", "Message envoyé.", "Mensagem enviada.", "Сообщение отправлено.", "メッセージを送信しました。"),
    Phrase("Sohbet odasına girmek için oturum açmalısın.", "Sign in to enter the chat room.", "Melde dich an, um den Chatraum zu betreten.", "Inicia sesión para entrar a la sala.", "Connecte-toi pour entrer dans le salon.", "Entre para acessar a sala.", "Войдите, чтобы зайти в чат.", "チャットルームに入るにはログインしてください。"),
    Phrase("Henüz yorum yok. İlk yorumu sen yaz.", "No comments yet. Write the first one.", "Noch keine Kommentare. Schreib den ersten.", "Aún no hay comentarios. Escribe el primero.", "Pas encore de commentaires. Écris le premier.", "Ainda não há comentários. Escreva o primeiro.", "Комментариев пока нет. Напишите первый.", "まだコメントがありません。最初のコメントを書こう。"),
    Phrase("Yorum yazmak için oturum aç", "Sign in to comment", "Melde dich an, um zu kommentieren", "Inicia sesión para comentar", "Connecte-toi pour commenter", "Entre para comentar", "Войдите, чтобы комментировать", "コメントするにはログイン"),

    /* ------------------------------------------------ contact */
    Phrase("Mesajın", "Your message", "Deine Nachricht", "Tu mensaje", "Ton message", "Sua mensagem", "Ваше сообщение", "あなたのメッセージ"),
    Phrase("Mesajın site yöneticilerine iletilir. En fazla 10000 karakter.", "Your message goes to the site admins. Max 10000 characters.", "Deine Nachricht geht an die Website-Admins. Max. 10000 Zeichen.", "Tu mensaje llega a los admins del sitio. Máx. 10000 caracteres.", "Ton message va aux admins du site. 10000 caractères max.", "Sua mensagem vai para os admins do site. Máx. 10000 caracteres.", "Сообщение уйдёт админам сайта. Не более 10000 символов.", "メッセージはサイト管理者に届きます。最大10000文字。"),
    Phrase("Hole moderatörlerine", "To the hole moderators", "An die Hole-Moderatoren", "A los moderadores del hole", "Aux modérateurs du hole", "Aos moderadores do hole", "Модераторам хола", "ホールのモデレーターへ"),

    /* ------------------------------------------------ lines with a value in them */
    Phrase("Çevrimiçi", "Online", "Online", "En línea", "En ligne", "Online", "В сети", "オンライン"),
    Phrase("Çevrimiçi: {0}", "Online: {0}", "Online: {0}", "En línea: {0}", "En ligne : {0}", "Online: {0}", "В сети: {0}", "オンライン: {0}"),
    Phrase("Seste {0}", "In voice {0}", "Im Sprachchat {0}", "En voz {0}", "En vocal {0}", "Em voz {0}", "В голосе {0}", "ボイス {0}"),
    Phrase("Seste {0} kişi", "In voice: {0}", "Im Sprachchat: {0}", "En voz: {0}", "En vocal : {0}", "Em voz: {0}", "В голосе: {0}", "ボイス: {0}人"),
    Phrase("{0} gönderi", "{0} posts", "{0} Beiträge", "{0} publicaciones", "{0} publications", "{0} publicações", "{0} постов", "{0}件の投稿"),
    Phrase("{0} yorum", "{0} comments", "{0} Kommentare", "{0} comentarios", "{0} commentaires", "{0} comentários", "{0} комментариев", "{0}件のコメント"),
    Phrase("{0} katıldı", "joined {0}", "dabei seit {0}", "se unió el {0}", "inscrit le {0}", "entrou em {0}", "с нами с {0}", "{0}に参加"),
    Phrase("katıldı: {0}", "joined: {0}", "dabei seit: {0}", "se unió: {0}", "inscrit : {0}", "entrou: {0}", "с нами: {0}", "参加: {0}"),
    Phrase("Katılım: {0}", "Joined: {0}", "Dabei seit: {0}", "Se unió: {0}", "Inscrit : {0}", "Entrou: {0}", "Регистрация: {0}", "登録: {0}"),
    Phrase("Herkese açık: {0}", "Public: {0}", "Öffentlich: {0}", "Público: {0}", "Public : {0}", "Público: {0}", "Публично: {0}", "公開: {0}"),
    Phrase("/h/{0} için gönderi yok.", "No posts in /h/{0} yet.", "Noch keine Beiträge in /h/{0}.", "Aún no hay publicaciones en /h/{0}.", "Pas encore de publications dans /h/{0}.", "Ainda não há publicações em /h/{0}.", "В /h/{0} пока нет постов.", "/h/{0} にはまだ投稿がありません。"),
    Phrase("@{0} kişisine yanıt yazıyorsun", "Replying to @{0}", "Antwort an @{0}", "Respondiendo a @{0}", "Réponse à @{0}", "Respondendo a @{0}", "Ответ @{0}", "@{0} への返信"),
    Phrase("Rastgele hedef bulunamadı: {0}", "Couldn't pick a random target: {0}", "Kein zufälliges Ziel gefunden: {0}", "No se encontró un objetivo aleatorio: {0}", "Aucune cible aléatoire trouvée : {0}", "Não foi possível sortear um alvo: {0}", "Не удалось выбрать случайную цель: {0}", "ランダムな対象が見つかりません: {0}"),

    /* ------------------------------------------------ failures, from the data layer */
    Phrase("İçerik", "Content", "Inhalt", "Contenido", "Contenu", "Conteúdo", "Содержимое", "コンテンツ"),
    Phrase("İşlem", "Action", "Aktion", "Acción", "Action", "Ação", "Действие", "操作"),
    Phrase("Akış", "Feed", "Feed", "Feed", "Flux", "Feed", "Лента", "フィード"),
    Phrase("Mesaj", "Message", "Nachricht", "Mensaje", "Message", "Mensagem", "Сообщение", "メッセージ"),
    Phrase("Takip", "Follow", "Folgen", "Seguir", "Suivre", "Seguir", "Подписка", "フォロー"),
    Phrase("Takipçi", "Followers", "Follower", "Seguidores", "Abonnés", "Seguidores", "Подписчики", "フォロワー"),
    Phrase("Takip Ediliyor", "Following", "Du folgst", "Siguiendo", "Abonné", "Seguindo", "Вы подписаны", "フォロー中"),
    Phrase("(başlıksız)", "(untitled)", "(ohne Titel)", "(sin título)", "(sans titre)", "(sem título)", "(без заголовка)", "（無題）"),
    Phrase("Siteye ulaşılamadı. İnternet bağlantını kontrol et.", "The site could not be reached. Check your internet connection.", "Die Website war nicht erreichbar. Prüfe deine Internetverbindung.", "No se pudo alcanzar el sitio. Revisa tu conexión a internet.", "Le site est injoignable. Vérifie ta connexion internet.", "Não foi possível alcançar o site. Verifique sua internet.", "Сайт недоступен. Проверьте интернет-соединение.", "サイトに接続できませんでした。インターネット接続を確認してください。"),
    Phrase("Sunucuya bağlanılamadı. Bağlantın kısıtlı olabilir (VPN/proxy).", "The server could not be reached. Your connection may be restricted (VPN/proxy).", "Der Server war nicht erreichbar. Deine Verbindung ist vielleicht eingeschränkt (VPN/Proxy).", "No se pudo conectar al servidor. Tu conexión puede estar restringida (VPN/proxy).", "Impossible de joindre le serveur. Ta connexion est peut-être restreinte (VPN/proxy).", "Não foi possível conectar ao servidor. Sua conexão pode estar restrita (VPN/proxy).", "Не удалось подключиться к серверу. Возможно, соединение ограничено (VPN/прокси).", "サーバーに接続できませんでした。接続が制限されている可能性があります（VPN/プロキシ）。"),
    Phrase("Güvenli bağlantı kurulamadı. Ağ bir sertifikaya müdahale ediyor olabilir.", "A secure connection could not be established. Something on the network may be interfering with the certificate.", "Eine sichere Verbindung kam nicht zustande. Etwas im Netz greift vielleicht in das Zertifikat ein.", "No se pudo establecer una conexión segura. Algo en la red puede estar interfiriendo con el certificado.", "La connexion sécurisée n'a pas pu s'établir. Quelque chose sur le réseau interfère peut-être avec le certificat.", "Não foi possível uma conexão segura. Algo na rede pode estar interferindo no certificado.", "Не удалось установить защищённое соединение. Возможно, кто-то вмешивается в сертификат.", "安全な接続を確立できませんでした。ネットワークが証明書に干渉している可能性があります。"),
    Phrase("Bağlantı yarıda kesildi. Tekrar dene.", "The connection dropped. Try again.", "Die Verbindung brach ab. Versuch es erneut.", "La conexión se cortó. Reintenta.", "La connexion a été coupée. Réessaie.", "A conexão caiu. Tente de novo.", "Соединение прервано. Повторите.", "接続が切れました。もう一度お試しください。"),
    Phrase("Delidehşet zamanında yanıt vermedi. Site beta olduğu için arada yavaşlıyor — tekrar dene.", "Delidehşet did not answer in time. The site is in beta, so it slows down now and then — try again.", "Delidehşet hat nicht rechtzeitig geantwortet. Die Seite ist in der Beta und wird manchmal langsam — versuch es erneut.", "Delidehşet no respondió a tiempo. El sitio está en beta y a veces va lento: reintenta.", "Delidehşet n'a pas répondu à temps. Le site est en bêta et ralentit parfois — réessaie.", "O Delidehşet não respondeu a tempo. O site está em beta e às vezes fica lento — tente de novo.", "Delidehşet не ответил вовремя. Сайт в бете и иногда тормозит — попробуйте ещё раз.", "Delidehşetが時間内に応答しませんでした。サイトはベータ版で時々遅くなります。もう一度お試しください。"),
    Phrase("Sunucu isteği reddetti (403). Büyük olasılıkla güvenlik doğrulaması eskidi — sayfayı yenileyip tekrar dene.", "The server refused the request (403). The security check has probably expired — refresh and try again.", "Der Server hat die Anfrage abgelehnt (403). Die Sicherheitsprüfung ist vermutlich abgelaufen — neu laden und erneut versuchen.", "El servidor rechazó la petición (403). La verificación de seguridad probablemente caducó: recarga y reintenta.", "Le serveur a refusé la requête (403). La vérification de sécurité a probablement expiré — recharge et réessaie.", "O servidor recusou a requisição (403). A verificação de segurança provavelmente expirou — recarregue e tente de novo.", "Сервер отклонил запрос (403). Скорее всего, проверка безопасности устарела — обновите страницу и повторите.", "サーバーがリクエストを拒否しました（403）。セキュリティ確認の期限が切れた可能性があります。更新して再試行してください。"),
    Phrase("İstek reddedildi (403). Güvenlik doğrulaması yenilenemedi, tekrar dene.", "The request was refused (403). The security check could not be renewed; try again.", "Die Anfrage wurde abgelehnt (403). Die Sicherheitsprüfung ließ sich nicht erneuern; versuch es erneut.", "La petición fue rechazada (403). No se pudo renovar la verificación de seguridad; reintenta.", "La requête a été refusée (403). La vérification de sécurité n'a pas pu être renouvelée ; réessaie.", "A requisição foi recusada (403). Não foi possível renovar a verificação de segurança; tente de novo.", "Запрос отклонён (403). Не удалось обновить проверку безопасности; повторите.", "リクエストが拒否されました（403）。セキュリティ確認を更新できませんでした。再試行してください。"),
    Phrase("Sunucu isteği zaman aşımına uğrattı (408). Tekrar dene.", "The server timed the request out (408). Try again.", "Der Server hat die Anfrage in einen Timeout laufen lassen (408). Versuch es erneut.", "El servidor agotó el tiempo de la petición (408). Reintenta.", "Le serveur a mis la requête en délai dépassé (408). Réessaie.", "O servidor estourou o tempo da requisição (408). Tente de novo.", "Сервер прервал запрос по таймауту (408). Повторите.", "サーバーがリクエストをタイムアウトしました（408）。もう一度お試しください。"),
    Phrase("Çok hızlı istek gönderdin (429). Birkaç saniye bekleyip tekrar dene.", "You sent requests too quickly (429). Wait a few seconds and try again.", "Du hast zu schnell angefragt (429). Warte ein paar Sekunden und versuch es erneut.", "Enviaste peticiones demasiado rápido (429). Espera unos segundos y reintenta.", "Tu as envoyé des requêtes trop vite (429). Attends quelques secondes et réessaie.", "Você enviou requisições rápido demais (429). Espere alguns segundos e tente de novo.", "Слишком частые запросы (429). Подождите несколько секунд и повторите.", "リクエストが速すぎます（429）。数秒待って再試行してください。"),
    Phrase("İşlem tamamlanamadı. Site beta olduğu için arada hata veriyor.", "The action could not be completed. The site is in beta, so it errors now and then.", "Die Aktion konnte nicht abgeschlossen werden. Die Seite ist in der Beta und fehlerhaft ab und zu.", "No se pudo completar la acción. El sitio está en beta y falla a veces.", "L'action n'a pas pu aboutir. Le site est en bêta et échoue parfois.", "A ação não pôde ser concluída. O site está em beta e falha às vezes.", "Действие не выполнено. Сайт в бете и иногда сбоит.", "操作を完了できませんでした。サイトはベータ版で時々エラーになります。"),
    Phrase("Beklenmeyen bir hata oluştu.", "Something unexpected went wrong.", "Etwas Unerwartetes ist schiefgelaufen.", "Ocurrió un error inesperado.", "Une erreur inattendue est survenue.", "Ocorreu um erro inesperado.", "Произошла неожиданная ошибка.", "予期しないエラーが発生しました。"),
    Phrase("Bu bölüm için oturum açman gerekiyor. Profil sekmesinden giriş yapabilirsin.", "This section needs a signed-in account. You can sign in from the Profile tab.", "Dieser Bereich braucht ein Konto. Anmelden kannst du dich im Profil-Tab.", "Esta sección necesita una sesión. Puedes entrar desde la pestaña Perfil.", "Cette section nécessite une connexion. Tu peux te connecter depuis l'onglet Profil.", "Esta seção precisa de login. Você pode entrar pela aba Perfil.", "Для этого раздела нужно войти. Это можно на вкладке «Профиль».", "このセクションにはログインが必要です。「プロフィール」タブから行えます。"),
    Phrase("Bu işlem için oturum açmalısın.", "You must sign in for this.", "Dafür musst du angemeldet sein.", "Debes iniciar sesión para esto.", "Tu dois te connecter pour cela.", "Você precisa entrar para isso.", "Для этого действия нужно войти.", "この操作にはログインが必要です。"),
    Phrase("Bu akış için oturum açman gerekiyor.", "You must sign in for this feed.", "Für diesen Feed musst du angemeldet sein.", "Debes iniciar sesión para este feed.", "Tu dois te connecter pour ce flux.", "Você precisa entrar para este feed.", "Для этой ленты нужно войти.", "このフィードにはログインが必要です。"),
    Phrase("Kullanıcı adı ve şifre gerekli.", "A username and a password are required.", "Benutzername und Passwort sind nötig.", "Se requieren nombre de usuario y contraseña.", "Un nom d'utilisateur et un mot de passe sont requis.", "Nome de usuário e senha são obrigatórios.", "Нужны имя пользователя и пароль.", "ユーザー名とパスワードが必要です。"),
    Phrase("Kullanıcı adı veya şifre hatalı.", "Wrong username or password.", "Benutzername oder Passwort ist falsch.", "Usuario o contraseña incorrectos.", "Nom d'utilisateur ou mot de passe incorrect.", "Usuário ou senha incorretos.", "Неверное имя пользователя или пароль.", "ユーザー名またはパスワードが違います。"),
    Phrase("Giriş yapılamadı. Kullanıcı adı veya şifre hatalı olabilir.", "Sign-in failed. The username or password may be wrong.", "Anmeldung fehlgeschlagen. Benutzername oder Passwort stimmt vielleicht nicht.", "No se pudo iniciar sesión. El usuario o la contraseña pueden ser incorrectos.", "Connexion impossible. Le nom d'utilisateur ou le mot de passe est peut-être faux.", "Falha ao entrar. O usuário ou a senha podem estar errados.", "Не удалось войти. Возможно, имя пользователя или пароль неверны.", "ログインできませんでした。ユーザー名またはパスワードが違う可能性があります。"),
    Phrase("Başlık ve topluluk zorunlu.", "A title and a community are required.", "Titel und Community sind erforderlich.", "Se requieren título y comunidad.", "Un titre et une communauté sont obligatoires.", "Título e comunidade são obrigatórios.", "Нужны заголовок и сообщество.", "タイトルとコミュニティは必須です。"),
    Phrase("Anonim gönderi için tripcode gizli değeri gerekli.", "An anonymous post needs the tripcode secret value.", "Ein anonymer Beitrag braucht den geheimen Tripcode-Wert.", "Una publicación anónima necesita el valor secreto del tripcode.", "Une publication anonyme exige la valeur secrète du tripcode.", "Uma publicação anônima precisa do valor secreto do tripcode.", "Для анонимного поста нужно секретное значение tripcode.", "匿名投稿にはトリップコードの秘密値が必要です。"),
    Phrase("Gönderi paylaşılamadı. Bağlantını kontrol edip tekrar dene.", "The post could not be shared. Check your connection and try again.", "Der Beitrag konnte nicht geteilt werden. Prüfe deine Verbindung und versuch es erneut.", "No se pudo compartir la publicación. Revisa tu conexión y reintenta.", "La publication n'a pas pu être partagée. Vérifie ta connexion et réessaie.", "Não foi possível publicar. Verifique sua conexão e tente de novo.", "Не удалось опубликовать пост. Проверьте соединение и повторите.", "投稿を共有できませんでした。接続を確認して再試行してください。"),
    Phrase("Gönderi sayfası beklenen biçimde gelmedi; site güncellenmiş olabilir.", "The post page did not arrive as expected; the site may have changed.", "Die Beitragsseite kam nicht wie erwartet; die Seite wurde vielleicht geändert.", "La página de la publicación no llegó como se esperaba; puede que el sitio haya cambiado.", "La page de la publication n'est pas arrivée comme prévu ; le site a peut-être changé.", "A página da publicação não veio como esperado; o site pode ter mudado.", "Страница поста пришла не в ожидаемом виде; возможно, сайт изменился.", "投稿ページが想定どおりに取得できませんでした。サイトが変更された可能性があります。"),
    Phrase("Gönderi silindi.", "Post deleted.", "Beitrag gelöscht.", "Publicación eliminada.", "Publication supprimée.", "Publicação excluída.", "Пост удалён.", "投稿を削除しました。"),
    Phrase("Yorum gönderildi.", "Comment posted.", "Kommentar gesendet.", "Comentario publicado.", "Commentaire publié.", "Comentário enviado.", "Комментарий отправлен.", "コメントを投稿しました。"),
    Phrase("Yorum gönderilemedi.", "The comment could not be posted.", "Der Kommentar konnte nicht gesendet werden.", "No se pudo publicar el comentario.", "Le commentaire n'a pas pu être publié.", "Não foi possível enviar o comentário.", "Не удалось отправить комментарий.", "コメントを投稿できませんでした。"),
    Phrase("Mesaj gönderilemedi. Tekrar dene.", "The message could not be sent. Try again.", "Die Nachricht konnte nicht gesendet werden. Versuch es erneut.", "No se pudo enviar el mensaje. Reintenta.", "Le message n'a pas pu être envoyé. Réessaie.", "Não foi possível enviar a mensagem. Tente de novo.", "Не удалось отправить сообщение. Повторите.", "メッセージを送信できませんでした。もう一度お試しください。"),
    Phrase("Şikâyet iletildi.", "Report sent.", "Meldung gesendet.", "Denuncia enviada.", "Signalement envoyé.", "Denúncia enviada.", "Жалоба отправлена.", "通報を送信しました。"),
    Phrase("Şifre değiştirildi.", "Password changed.", "Passwort geändert.", "Contraseña cambiada.", "Mot de passe changé.", "Senha alterada.", "Пароль изменён.", "パスワードを変更しました。"),
    Phrase("E-posta güncellendi.", "Email updated.", "E-Mail aktualisiert.", "Correo actualizado.", "E-mail mis à jour.", "E-mail atualizado.", "Почта обновлена.", "メールを更新しました。"),
    Phrase("Profil fotoğrafı güncellendi.", "Profile picture updated.", "Profilbild aktualisiert.", "Foto de perfil actualizada.", "Photo de profil mise à jour.", "Foto de perfil atualizada.", "Фото профиля обновлено.", "プロフィール画像を更新しました。"),
    Phrase("Banner güncellendi.", "Banner updated.", "Banner aktualisiert.", "Banner actualizado.", "Bannière mise à jour.", "Banner atualizado.", "Баннер обновлён.", "バナーを更新しました。"),
    Phrase("Yeni e-posta ve şifre gerekli.", "The new email and the password are required.", "Die neue E-Mail und das Passwort sind nötig.", "Se requieren el nuevo correo y la contraseña.", "Le nouvel e-mail et le mot de passe sont requis.", "O novo e-mail e a senha são obrigatórios.", "Нужны новая почта и пароль.", "新しいメールアドレスとパスワードが必要です。"),
    Phrase("Yeni şifreler birbiriyle uyuşmuyor.", "The new passwords do not match.", "Die neuen Passwörter stimmen nicht überein.", "Las nuevas contraseñas no coinciden.", "Les nouveaux mots de passe ne correspondent pas.", "As novas senhas não coincidem.", "Новые пароли не совпадают.", "新しいパスワードが一致しません。"),
    Phrase("Çıkış yapıldı.", "Signed out.", "Abgemeldet.", "Sesión cerrada.", "Déconnecté.", "Sessão encerrada.", "Вы вышли.", "ログアウトしました。"),
    Phrase("ağ hatası", "network error", "Netzwerkfehler", "error de red", "erreur réseau", "erro de rede", "ошибка сети", "ネットワークエラー"),
    Phrase("{0} isteği sunucu tarafından reddedildi (400). Alanları kontrol et.", "The {0} request was refused by the server (400). Check the fields.", "Die Anfrage ({0}) wurde vom Server abgelehnt (400). Prüfe die Felder.", "El servidor rechazó la petición de {0} (400). Revisa los campos.", "La requête {0} a été refusée par le serveur (400). Vérifie les champs.", "O servidor recusou a requisição de {0} (400). Verifique os campos.", "Сервер отклонил запрос «{0}» (400). Проверьте поля.", "{0}のリクエストがサーバーに拒否されました（400）。入力内容を確認してください。"),
    Phrase("{0} için oturum açman gerekiyor (401).", "You must sign in for {0} (401).", "Für {0} musst du angemeldet sein (401).", "Debes iniciar sesión para {0} (401).", "Tu dois te connecter pour {0} (401).", "Você precisa entrar para {0} (401).", "Для «{0}» нужно войти (401).", "{0}にはログインが必要です（401）。"),
    Phrase("{0} bulunamadı (404). Ya kaldırılmış ya da henüz açılmamış bir bölüm.", "{0} was not found (404). Either it was removed or it is not open yet.", "{0} wurde nicht gefunden (404). Entweder entfernt oder noch nicht geöffnet.", "No se encontró {0} (404). O se eliminó o aún no está abierto.", "{0} introuvable (404). Soit c'est supprimé, soit ce n'est pas encore ouvert.", "{0} não foi encontrado (404). Ou foi removido ou ainda não abriu.", "«{0}» не найдено (404). Либо удалено, либо ещё не открыто.", "{0}が見つかりません（404）。削除されたか、まだ公開されていません。"),
    Phrase("{0} bu şekilde açılmıyor (405). Bu bölüm yalnızca form gönderimiyle çalışıyor.", "{0} does not open this way (405). This section only works through a form submission.", "{0} öffnet sich so nicht (405). Dieser Bereich funktioniert nur über ein Formular.", "{0} no se abre así (405). Esta sección solo funciona enviando un formulario.", "{0} ne s'ouvre pas ainsi (405). Cette section ne fonctionne que par envoi de formulaire.", "{0} não abre assim (405). Esta seção só funciona com envio de formulário.", "«{0}» так не открывается (405). Этот раздел работает только через отправку формы.", "{0}はこの方法では開けません（405）。このセクションはフォーム送信でのみ動作します。"),
    Phrase("{0} yüklenemedi (HTTP {1}).", "{0} could not be loaded (HTTP {1}).", "{0} konnte nicht geladen werden (HTTP {1}).", "No se pudo cargar {0} (HTTP {1}).", "{0} n'a pas pu être chargé (HTTP {1}).", "Não foi possível carregar {0} (HTTP {1}).", "Не удалось загрузить «{0}» (HTTP {1}).", "{0}を読み込めませんでした（HTTP {1}）。"),
    Phrase("Delidehşet şu anda hata veriyor (HTTP {0}). Site beta olduğu için bu olabiliyor — birazdan tekrar dene.", "Delidehşet is erroring right now (HTTP {0}). That happens while the site is in beta — try again in a moment.", "Delidehşet gibt gerade Fehler zurück (HTTP {0}). Das kommt in der Beta vor — versuch es gleich erneut.", "Delidehşet está dando error ahora mismo (HTTP {0}). Pasa mientras el sitio está en beta: reintenta en un momento.", "Delidehşet renvoie une erreur en ce moment (HTTP {0}). Ça arrive en bêta — réessaie dans un instant.", "O Delidehşet está com erro agora (HTTP {0}). Acontece enquanto o site está em beta — tente de novo em instantes.", "Delidehşet сейчас отдаёт ошибку (HTTP {0}). Пока сайт в бете это бывает — повторите чуть позже.", "Delidehşetが現在エラーを返しています（HTTP {0}）。ベータ版ではよくあることです。少しして再試行してください。"),
    Phrase("Bağlantı kurulamadı: {0}", "Could not connect: {0}", "Keine Verbindung: {0}", "No se pudo conectar: {0}", "Connexion impossible : {0}", "Não foi possível conectar: {0}", "Не удалось подключиться: {0}", "接続できませんでした: {0}"),
    Phrase("Giriş yapılamadı (HTTP {0}).", "Sign-in failed (HTTP {0}).", "Anmeldung fehlgeschlagen (HTTP {0}).", "No se pudo iniciar sesión (HTTP {0}).", "Connexion impossible (HTTP {0}).", "Falha ao entrar (HTTP {0}).", "Не удалось войти (HTTP {0}).", "ログインできませんでした（HTTP {0}）。"),
    Phrase("{0} için oturum açman gerekiyor.", "You must sign in for {0}.", "Für {0} musst du angemeldet sein.", "Debes iniciar sesión para {0}.", "Tu dois te connecter pour {0}.", "Você precisa entrar para {0}.", "Для «{0}» нужно войти.", "{0}にはログインが必要です。"),
)

/**
 * Fills `{0}`, `{1}` … into a line. The values are inserted after the wording has been
 * translated, so no name, room or count is ever fed to the dictionary.
 */
fun fill(line: String, vararg values: Any?): String {
    var out = line
    values.forEachIndexed { index, value -> out = out.replace("{$index}", value?.toString().orEmpty()) }
    return out
}

/**
 * The language in force right now, for the layers that are not composables.
 *
 * Errors, results and messages are built in the data layer and shown later, so they cannot
 * read the composition the way a screen does. [AppViewModel] keeps this in step with the
 * chosen language, and [tr] / [trf] read it.
 */
object CurrentLang {
    @Volatile
    var code: String = AppLang.TR.code

    val lang: AppLang get() = AppLang.of(code)
}

/** The line, for a caller outside the composition — see [CurrentLang]. */
fun tr(text: String): String = translate(CurrentLang.lang, text)

/** A line with values in it, for a caller outside the composition. */
fun trf(template: String, vararg values: Any?): String = fill(tr(template), *values)

private val byLang: Map<AppLang, Map<String, String>> by lazy {
    AppLang.entries.associateWith { lang ->
        val map = HashMap<String, String>(phrases.size * 2)
        for (phrase in phrases) map[phrase.tr] = phrase.inLang(lang)
        map
    }
}
