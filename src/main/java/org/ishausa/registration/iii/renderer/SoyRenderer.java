package org.ishausa.registration.iii.renderer;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;

import java.io.File;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Helper that knows how to render content using the Soy templates.
 *
 * Created by tosri on 3/5/2017.
 */
public class SoyRenderer {
    private static final Logger log = Logger.getLogger(SoyRenderer.class.getName());

    public enum RegistrationAppTemplate {
        INDEX,
        NON_EXISTENT_ID;

        @Override
        public String toString() {
            return "." + name().toLowerCase();
        }
    }

    public static final SoyRenderer INSTANCE = new SoyRenderer();

    private final SoyTofu serviceTofu;

    private SoyRenderer() {
        final SoyFileSet sfs = SoyFileSet.builder()
                .add(new File("./src/main/webapp/template/registration_app.soy"))
                .build();
        serviceTofu = sfs.compileToTofu().forNamespace("org.ishausa.registration.iii");
    }

    public String render(final RegistrationAppTemplate template, final Map<String, ?> data) {
        log.info("Rendering template: " + template + " with data: " + data);
        return serviceTofu.newRenderer(template.toString()).setData(data).render();
    }

    public static String calendarToString(final Calendar cal) {
        return cal.getTime()
                .toInstant()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_DATE);
    }
}
