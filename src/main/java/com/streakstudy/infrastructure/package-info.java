/**
 * Capa de Infraestructura.
 *
 * <p>Contiene los adaptadores que conectan el sistema con el mundo exterior:
 * controllers HTTP, implementaciones JPA de repositorios, clientes a servicios
 * externos, configuracion de seguridad, etc.</p>
 *
 * <p>Puede depender de {@code domain} y {@code application}.</p>
 *
 * <p>Subpaquetes esperados:</p>
 * <ul>
 *   <li>{@code web} — Controllers REST y advices.</li>
 *   <li>{@code persistence} — Entidades JPA y repositorios Spring Data.</li>
 *   <li>{@code security} — Configuracion de Spring Security y filtros.</li>
 *   <li>{@code config} — Beans y configuracion general de Spring.</li>
 * </ul>
 */
package com.streakstudy.infrastructure;
