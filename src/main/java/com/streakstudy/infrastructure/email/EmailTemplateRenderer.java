package com.streakstudy.infrastructure.email;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renderiza plantillas Thymeleaf bajo {@code classpath:/templates/email/}.
 *
 * <p>Se separa del adapter SMTP para poder testear el renderizado sin
 * abrir una conexion de correo.</p>
 */
@Component
public class EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    public EmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateName, Map<String, Object> model) {
        Context ctx = new Context();
        if (model != null) {
            ctx.setVariables(model);
        }
        return templateEngine.process("email/" + templateName, ctx);
    }
}
