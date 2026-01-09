use askama::Template;
use axum::{
    extract::Query,
    http::HeaderMap,
    response::Redirect,
};
use askama_axum::IntoResponse;
use serde::Deserialize;

#[derive(Template)]
#[template(path = "settings.html.jinja")]
pub(crate) struct SettingsTemplate {
    theme: String,
}

#[derive(Deserialize)]
pub(crate) struct ThemeQuery {
    theme: Option<String>,
}

fn get_theme_from_cookie(headers: &HeaderMap) -> String {
    if let Some(cookie_header) = headers.get("cookie") {
        if let Ok(cookie_str) = cookie_header.to_str() {
            for cookie in cookie_str.split(';') {
                let cookie = cookie.trim();
                if let Some((name, value)) = cookie.split_once('=') {
                    if name.trim() == "theme" {
                        return value.trim().to_string();
                    }
                }
            }
        }
    }
    "light".to_string()
}

pub(crate) async fn get_settings(headers: HeaderMap) -> SettingsTemplate {
    let theme = get_theme_from_cookie(&headers);
    SettingsTemplate { theme }
}

pub(crate) async fn post_settings(Query(params): Query<ThemeQuery>) -> impl IntoResponse {
    // Theme will be set via JavaScript on client side
    // This endpoint just redirects - the actual cookie setting happens in the browser
    let _theme = params.theme.unwrap_or_else(|| "light".to_string());
    
    // Redirect back to settings page
    Redirect::to("/settings")
}
