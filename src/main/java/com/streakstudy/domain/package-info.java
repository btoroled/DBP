/**
 * Capa de Dominio.
 *
 * <p>Contiene las entidades de negocio puras y las reglas que las gobiernan.
 * No depende de Spring, JPA ni de ningun framework de infraestructura.
 * Solo puede usar Java estandar y, opcionalmente, librerias de utileria
 * (Lombok, etc.) que no aten al dominio a un framework.</p>
 *
 * <p>Subpaquetes esperados:</p>
 * <ul>
 *   <li>{@code model} — Entidades, value objects, agregados.</li>
 *   <li>{@code repository} — Interfaces de repositorio (puertos).</li>
 *   <li>{@code exception} — Excepciones propias del dominio.</li>
 * </ul>
 */
package com.streakstudy.domain;
