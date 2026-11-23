package com.orbi.orbi_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.core.app.ApplicationProvider;

import com.orbi.orbi_app.model.RouteStatus;
import com.orbi.orbi_app.model.WaypointStatus;
import com.orbi.orbi_app.ui.common.StatusStyles;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
public class DesignSystemResourcesTest {

    @Test
    @Config(qualifiers = "notnight")
    public void lightThemeTokensMatchDesignSystem() {
        Context context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Orbi_app
        );

        assertEquals(Color.parseColor("#2557D6"), ContextCompat.getColor(context, R.color.primary));
        assertEquals(Color.parseColor("#FFFFFF"), ContextCompat.getColor(context, R.color.on_primary));
        assertEquals(Color.parseColor("#F7F7F5"), ContextCompat.getColor(context, R.color.background));
        assertEquals(Color.parseColor("#FFFFFF"), ContextCompat.getColor(context, R.color.surface));
        assertEquals(Color.parseColor("#0A0A0A"), ContextCompat.getColor(context, R.color.on_surface));
        assertEquals(Color.parseColor("#E5E5E3"), ContextCompat.getColor(context, R.color.card_stroke));
        assertEquals(Color.parseColor("#EEF3FD"), ContextCompat.getColor(context, R.color.primary_soft));
        assertEquals(Color.parseColor("#C9D7F7"), ContextCompat.getColor(context, R.color.primary_line));
        assertEquals(Color.parseColor("#146C2E"), ContextCompat.getColor(context, R.color.status_completed));

        assertEquals(Color.parseColor("#2557D6"), resolveThemeColor(context, androidx.appcompat.R.attr.colorPrimary));
        assertEquals(Color.parseColor("#FFFFFF"), resolveThemeColor(context, com.google.android.material.R.attr.colorOnPrimary));
    }

    @Test
    @Config(qualifiers = "night")
    public void darkThemeTokensUseCarbonBlackAndWhiteOnPrimary() {
        Context base = ApplicationProvider.getApplicationContext();
        Configuration nightConfig = new Configuration(base.getResources().getConfiguration());
        nightConfig.uiMode = (nightConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | Configuration.UI_MODE_NIGHT_YES;
        Context context = new ContextThemeWrapper(
                base.createConfigurationContext(nightConfig),
                R.style.Theme_Orbi_app
        );

        assertEquals(Color.parseColor("#3B6DF0"), ContextCompat.getColor(context, R.color.primary));
        assertEquals(Color.parseColor("#FFFFFF"), ContextCompat.getColor(context, R.color.on_primary));
        assertEquals(Color.parseColor("#93B4FF"), ContextCompat.getColor(context, R.color.primary_text));
        assertEquals(Color.parseColor("#0A0A0A"), ContextCompat.getColor(context, R.color.background));
        assertEquals(Color.parseColor("#141414"), ContextCompat.getColor(context, R.color.surface));
        assertEquals(Color.parseColor("#F7F7F5"), ContextCompat.getColor(context, R.color.on_surface));
        assertEquals(Color.parseColor("#2A2A2A"), ContextCompat.getColor(context, R.color.card_stroke));
        assertEquals(Color.parseColor("#6EE7B7"), ContextCompat.getColor(context, R.color.status_completed));
        assertEquals(Color.parseColor("#F5F5F5"), ContextCompat.getColor(context, R.color.nav_action_complete));
        assertEquals(Color.parseColor("#F5F5F5"), ContextCompat.getColor(context, R.color.nav_action_complete_text));
        assertEquals(Color.parseColor("#F5F5F5"), ContextCompat.getColor(context, R.color.nav_action_cancel_text));
        assertEquals(Color.parseColor("#F5F5F5"), ContextCompat.getColor(context, R.color.nav_action_skip_text));
        assertEquals(Color.parseColor("#FBBF24"), ContextCompat.getColor(context, R.color.nav_gps_warn));
        assertEquals(Color.parseColor("#F7F7F5"), ContextCompat.getColor(context, R.color.badge_dark_bg));
        assertEquals(Color.parseColor("#0A0A0A"), ContextCompat.getColor(context, R.color.badge_dark_fg));
    }

    @Test
    @Config(qualifiers = "notnight")
    public void wcag21ContrastPassesInLightMode() {
        Context context = createThemedContext(false);
        assertWcag21Contrast(context, "light");
        assertDarkButtonStyleContrast(context, "light",
                Color.parseColor("#0A0A0A"), Color.parseColor("#FFFFFF"));
    }

    @Test
    @Config(qualifiers = "night")
    public void wcag21ContrastPassesInDarkMode() {
        Context context = createThemedContext(true);
        assertWcag21Contrast(context, "night");
        assertDarkButtonStyleContrast(context, "night",
                Color.parseColor("#F7F7F5"), Color.parseColor("#0A0A0A"));
    }

    @Test
    public void fontsAndTabularFeatureSettingsLoadCleanly() {
        Context context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Orbi_app
        );

        Typeface inter = ResourcesCompat.getFont(context, R.font.inter);
        Typeface jetBrainsMono = ResourcesCompat.getFont(context, R.font.jetbrains_mono);
        assertNotNull("Inter font family should resolve offline", inter);
        assertNotNull("JetBrains Mono font family should resolve offline", jetBrainsMono);

        TextView statValue = new TextView(context);
        statValue.setTextAppearance(R.style.Widget_Orbi_StatValue);
        String features = statValue.getFontFeatureSettings();
        assertNotNull(features);
        assertTrue("StatValue should preserve tnum", features.contains("tnum"));
        assertTrue("StatValue should preserve cv11", features.contains("cv11"));
        assertTrue("StatValue should preserve ss01", features.contains("ss01"));
    }

    @Test
    public void statusStylesAppliesPillStrokeAndLeadingDot() {
        Context context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Orbi_app
        );
        TextView pill = new TextView(context);

        StatusStyles.Style completed = StatusStyles.of(RouteStatus.COMPLETED);
        assertEquals(R.color.badge_dark_fg, completed.foreground);
        assertEquals(R.color.badge_dark_bg, completed.background);
        assertEquals(R.color.badge_dark_border, completed.border);

        StatusStyles.applyPill(pill, completed);
        assertEquals(context.getString(R.string.status_route_completed), pill.getText().toString());
        assertEquals(ContextCompat.getColor(context, R.color.badge_dark_fg), pill.getCurrentTextColor());
        assertTrue("applyPill should set GradientDrawable background", pill.getBackground() instanceof GradientDrawable);

        Drawable[] drawables = pill.getCompoundDrawablesRelative();
        assertTrue("applyPill should set leading 6dp dot drawable", drawables[0] instanceof GradientDrawable);

        StatusStyles.Style skipped = StatusStyles.of(WaypointStatus.SKIPPED);
        assertEquals(R.color.status_skipped, skipped.foreground);
        assertEquals(R.color.badge_warning_border, skipped.border);
    }

    @Test
    public void holdHintsUseOneSecondAndNavStringsExist() {
        Context context = ApplicationProvider.getApplicationContext();

        String skipHint = context.getString(R.string.btn_hold_skip_hint);
        String cancelHint = context.getString(R.string.btn_hold_cancel_hint);
        assertTrue("Skip hint should reference 1s", skipHint.contains("1s"));
        assertFalse("Skip hint should not reference 3s", skipHint.contains("3s"));
        assertTrue("Cancel hint should reference 1s", cancelHint.contains("1s"));
        assertFalse("Cancel hint should not reference 3s", cancelHint.contains("3s"));

        assertEquals("A seguir", context.getString(R.string.nav_next_maneuver_label));
        assertEquals("Cliente / Destinatário", context.getString(R.string.nav_recipient_label));
        assertFalse(context.getString(R.string.error_sync_action_failed).isEmpty());
    }

    @Test
    public void mapStylesAreValidJsonAndDarkStyleHasNoRedHueOrInvertLightness() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        String lightJson = readRawText(context, R.raw.map_style);
        JSONArray lightArray = new JSONArray(lightJson);
        assertTrue(lightArray.length() > 0);

        String darkJson = readRawText(context, R.raw.map_style_dark);
        JSONArray darkArray = new JSONArray(darkJson);
        assertTrue(darkArray.length() > 0);
        assertFalse("Dark map style must not contain invert_lightness", darkJson.contains("invert_lightness"));
        assertFalse("Dark map style must not contain #ff1a00", darkJson.toLowerCase().contains("#ff1a00"));
    }

    @Test
    public void linearIconsAndSlideDrawablesInflateCleanly() {
        Context context = new ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                R.style.Theme_Orbi_app
        );
        int[] drawables = new int[] {
                R.drawable.ic_arrow_back, R.drawable.ic_cancel, R.drawable.ic_check,
                R.drawable.ic_chevron_right, R.drawable.ic_chevron_up, R.drawable.ic_close,
                R.drawable.ic_cloud_done, R.drawable.ic_cloud_off, R.drawable.ic_dns,
                R.drawable.ic_inbox, R.drawable.ic_info, R.drawable.ic_lock,
                R.drawable.ic_logout, R.drawable.ic_mail, R.drawable.ic_my_location,
                R.drawable.ic_navigation, R.drawable.ic_phone, R.drawable.ic_place,
                R.drawable.ic_route, R.drawable.ic_schedule, R.drawable.ic_skip,
                R.drawable.ic_turn_left, R.drawable.ic_turn_right, R.drawable.ic_turn_straight,
                R.drawable.bg_flat_slide_track, R.drawable.bg_flat_slide_thumb,
                R.drawable.bg_flat_slide_fill, R.drawable.bg_gps_badge,
                R.drawable.bg_turn_box, R.drawable.bg_rounded_container, R.drawable.bg_status_pill
        };
        for (int resId : drawables) {
            assertNotNull("Drawable resource should inflate: " + resId, ContextCompat.getDrawable(context, resId));
        }
    }

    @Test
    public void mockitoAndMockWebServerWorkOnTestRuntime() throws Exception {
        Runnable callback = mock(Runnable.class);
        callback.run();
        verify(callback).run();

        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(204));
            server.start();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(server.url("/api/deliveries/1/skip"))
                    .patch(okhttp3.RequestBody.create(new byte[0]))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                assertEquals(204, response.code());
            }
        }
    }

    private static Context createThemedContext(boolean night) {
        Context base = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (night ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return new ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Orbi_app);
    }

    private static void assertWcag21Contrast(Context context, String mode) {
        boolean isDark = "night".equals(mode) || "dark".equals(mode);
        int surface = ContextCompat.getColor(context, R.color.surface);
        int surfaceHigh = ContextCompat.getColor(context, R.color.surface_container_high);
        int background = ContextCompat.getColor(context, R.color.background);
        int primary = ContextCompat.getColor(context, R.color.primary);
        int primarySoft = ContextCompat.getColor(context, R.color.primary_soft);
        int white = ContextCompat.getColor(context, R.color.white);
        int navManeuverBg = ContextCompat.getColor(context, R.color.nav_maneuver_bg);
        int navManeuverSurface = ContextCompat.getColor(context, R.color.nav_maneuver_surface);

        // Text pairs (>= 4.5:1 WCAG AA)
        assertMinContrast(mode + " on_primary / primary",
                ContextCompat.getColor(context, R.color.on_primary), primary, 4.5);
        assertMinContrast(mode + " primary_text / surface",
                ContextCompat.getColor(context, R.color.primary_text), surface, 4.5);
        assertMinContrast(mode + " primary_text / primary_soft",
                ContextCompat.getColor(context, R.color.primary_text), primarySoft, 4.5);
        assertMinContrast(mode + " on_surface / surface",
                ContextCompat.getColor(context, R.color.on_surface), surface, 4.5);
        assertMinContrast(mode + " on_surface / background",
                ContextCompat.getColor(context, R.color.on_surface), background, 4.5);
        assertMinContrast(mode + " on_surface_variant / surface",
                ContextCompat.getColor(context, R.color.on_surface_variant), surface, 4.5);

        if (!isDark) {
            assertMinContrast(mode + " nav_action_complete / primary_soft",
                    ContextCompat.getColor(context, R.color.nav_action_complete), primarySoft, 4.5);
            assertMinContrast(mode + " nav_action_complete_text / primary_soft",
                    ContextCompat.getColor(context, R.color.nav_action_complete_text), primarySoft, 4.5);
        }
        assertMinContrast(mode + " nav_action_complete_text / surface",
                ContextCompat.getColor(context, R.color.nav_action_complete_text), surface, 4.5);

        int cancelText = ContextCompat.getColor(context, R.color.nav_action_cancel_text);
        int cancelSoft = compositeOver(ContextCompat.getColor(context, R.color.nav_action_cancel_soft), surface);
        assertMinContrast(mode + " nav_action_cancel_text / surface", cancelText, surface, 4.5);
        assertMinContrast(mode + " nav_action_cancel_text / surface_container_high", cancelText, surfaceHigh, 4.5);
        assertMinContrast(mode + " nav_action_cancel_text / nav_action_cancel_soft", cancelText, cancelSoft, 4.5);

        int skipText = ContextCompat.getColor(context, R.color.nav_action_skip_text);
        int skipSoft = compositeOver(ContextCompat.getColor(context, R.color.nav_action_skip_soft), surface);
        assertMinContrast(mode + " nav_action_skip_text / surface", skipText, surface, 4.5);
        assertMinContrast(mode + " nav_action_skip_text / surface_container_high", skipText, surfaceHigh, 4.5);
        assertMinContrast(mode + " nav_action_skip_text / nav_action_skip_soft", skipText, skipSoft, 4.5);

        if (!isDark) {
            assertMinContrast(mode + " white / nav_action_cancel",
                    white, ContextCompat.getColor(context, R.color.nav_action_cancel), 4.5);
            assertMinContrast(mode + " white / nav_action_skip",
                    white, ContextCompat.getColor(context, R.color.nav_action_skip), 4.5);
        } else {
            int black = ContextCompat.getColor(context, R.color.carbon_black);
            assertMinContrast(mode + " black / nav_action_cancel",
                    black, ContextCompat.getColor(context, R.color.nav_action_cancel), 4.5);
            assertMinContrast(mode + " black / nav_action_skip",
                    black, ContextCompat.getColor(context, R.color.nav_action_skip), 4.5);
        }

        int badgeDarkBg = ContextCompat.getColor(context, R.color.badge_dark_bg);
        int badgeDarkFg = ContextCompat.getColor(context, R.color.badge_dark_fg);
        assertMinContrast(mode + " badge_dark_fg / badge_dark_bg", badgeDarkFg, badgeDarkBg, 4.5);

        // UI & GPS dot boundary pairs (>= 3.0:1 WCAG AA non-text)
        assertMinContrast(mode + " nav_gps_warn / nav_maneuver_surface",
                ContextCompat.getColor(context, R.color.nav_gps_warn), navManeuverSurface, 3.0);
        assertMinContrast(mode + " nav_gps_warn / nav_maneuver_bg",
                ContextCompat.getColor(context, R.color.nav_gps_warn), navManeuverBg, 3.0);
        assertMinContrast(mode + " nav_gps_ok / nav_maneuver_surface",
                ContextCompat.getColor(context, R.color.nav_gps_ok), navManeuverSurface, 3.0);
        assertMinContrast(mode + " badge_dark_bg / surface", badgeDarkBg, surface, 3.0);
        assertMinContrast(mode + " badge_dark_bg / background", badgeDarkBg, background, 3.0);
    }

    private static void assertDarkButtonStyleContrast(
            Context context,
            String mode,
            int expectedBg,
            int expectedFg
    ) {
        TypedArray attrs = context.obtainStyledAttributes(
                R.style.Widget_Orbi_Button_Dark,
                new int[] {
                        com.google.android.material.R.attr.backgroundTint,
                        android.R.attr.textColor,
                        com.google.android.material.R.attr.iconTint
                }
        );
        try {
            int bgColor = attrs.getColor(0, 0);
            int textColor = attrs.getColor(1, 0);
            int iconColor = attrs.getColor(2, 0);
            assertEquals(mode + " Widget.Orbi.Button.Dark backgroundTint", expectedBg, bgColor);
            assertEquals(mode + " Widget.Orbi.Button.Dark textColor", expectedFg, textColor);
            assertEquals(mode + " Widget.Orbi.Button.Dark iconTint", expectedFg, iconColor);
            assertMinContrast(mode + " Button.Dark fg / bg", textColor, bgColor, 4.5);
            assertMinContrast(mode + " Button.Dark bg / surface",
                    bgColor, ContextCompat.getColor(context, R.color.surface), 3.0);
        } finally {
            attrs.recycle();
        }
    }

    private static void assertMinContrast(String label, int foreground, int background, double minRatio) {
        double ratio = contrastRatio(foreground, background);
        assertTrue(
                label + " contrast " + String.format("%.2f", ratio) + ":1 < " + minRatio + ":1",
                ratio >= minRatio
        );
    }

    private static double contrastRatio(int foreground, int background) {
        int opaqueFg = Color.alpha(foreground) < 255
                ? compositeOver(foreground, background)
                : foreground;
        double l1 = relativeLuminance(opaqueFg);
        double l2 = relativeLuminance(background);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(int color) {
        double r = linearizeChannel(Color.red(color) / 255.0);
        double g = linearizeChannel(Color.green(color) / 255.0);
        double b = linearizeChannel(Color.blue(color) / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearizeChannel(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static int compositeOver(int foreground, int background) {
        double alpha = Color.alpha(foreground) / 255.0;
        double invAlpha = 1.0 - alpha;
        int r = (int) Math.round(Color.red(foreground) * alpha + Color.red(background) * invAlpha);
        int g = (int) Math.round(Color.green(foreground) * alpha + Color.green(background) * invAlpha);
        int b = (int) Math.round(Color.blue(foreground) * alpha + Color.blue(background) * invAlpha);
        return Color.rgb(r, g, b);
    }

    private static int resolveThemeColor(Context context, int attr) {
        TypedValue tv = new TypedValue();
        assertTrue(context.getTheme().resolveAttribute(attr, tv, true));
        return tv.resourceId != 0 ? ContextCompat.getColor(context, tv.resourceId) : tv.data;
    }

    private static String readRawText(Context context, int rawResId) throws IOException {
        try (InputStream is = context.getResources().openRawResource(rawResId);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
