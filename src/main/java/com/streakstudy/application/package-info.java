/**
 * Capa de Aplicacion.
 *
 * <p>Orquesta los casos de uso del sistema: recibe DTOs desde la capa web,
 * coordina al dominio y delega persistencia a los puertos.</p>
 *
 * <p>Puede depender de {@code domain} pero NO de {@code infrastructure}.</p>
 *
 * <p>Subpaquetes esperados:</p>
 * <ul>
 *   <li>{@code service} — Servicios de aplicacion (orquestacion).</li>
 *   <li>{@code usecase} — Implementacion de casos de uso especificos.</li>
 *   <li>{@code dto} — Objetos de transferencia (request/response).</li>
 * </ul>
 */
package com.streakstudy.application;
