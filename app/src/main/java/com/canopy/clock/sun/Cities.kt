package com.canopy.clock.sun

/** Ciudades incluidas en la app (nombre, zona horaria y coordenadas). Sin conexión. */
data class City(val name: String, val country: String, val zone: String, val lat: Double, val lon: Double)

object Cities {
    val ALL = listOf(
        City("Madrid", "España", "Europe/Madrid", 40.4168, -3.7038),
        City("Barcelona", "España", "Europe/Madrid", 41.3874, 2.1686),
        City("Valencia", "España", "Europe/Madrid", 39.4699, -0.3763),
        City("Sevilla", "España", "Europe/Madrid", 37.3891, -5.9845),
        City("Bilbao", "España", "Europe/Madrid", 43.2630, -2.9350),
        City("Valladolid", "España", "Europe/Madrid", 41.6523, -4.7245),
        City("A Coruña", "España", "Europe/Madrid", 43.3623, -8.4115),
        City("Palma", "España", "Europe/Madrid", 39.5696, 2.6502),
        City("Las Palmas", "España", "Atlantic/Canary", 28.1235, -15.4363),
        City("Santa Cruz de Tenerife", "España", "Atlantic/Canary", 28.4636, -16.2518),
        City("Lisboa", "Portugal", "Europe/Lisbon", 38.7223, -9.1393),
        City("Londres", "Reino Unido", "Europe/London", 51.5072, -0.1276),
        City("París", "Francia", "Europe/Paris", 48.8566, 2.3522),
        City("Berlín", "Alemania", "Europe/Berlin", 52.5200, 13.4050),
        City("Fráncfort", "Alemania", "Europe/Berlin", 50.1109, 8.6821),
        City("Ámsterdam", "Países Bajos", "Europe/Amsterdam", 52.3676, 4.9041),
        City("Bruselas", "Bélgica", "Europe/Brussels", 50.8503, 4.3517),
        City("Roma", "Italia", "Europe/Rome", 41.9028, 12.4964),
        City("Milán", "Italia", "Europe/Rome", 45.4642, 9.1900),
        City("Zúrich", "Suiza", "Europe/Zurich", 47.3769, 8.5417),
        City("Viena", "Austria", "Europe/Vienna", 48.2082, 16.3738),
        City("Copenhague", "Dinamarca", "Europe/Copenhagen", 55.6761, 12.5683),
        City("Estocolmo", "Suecia", "Europe/Stockholm", 59.3293, 18.0686),
        City("Helsinki", "Finlandia", "Europe/Helsinki", 60.1699, 24.9384),
        City("Atenas", "Grecia", "Europe/Athens", 37.9838, 23.7275),
        City("Estambul", "Turquía", "Europe/Istanbul", 41.0082, 28.9784),
        City("Moscú", "Rusia", "Europe/Moscow", 55.7558, 37.6173),
        City("Marrakech", "Marruecos", "Africa/Casablanca", 31.6295, -7.9811),
        City("El Cairo", "Egipto", "Africa/Cairo", 30.0444, 31.2357),
        City("Lagos", "Nigeria", "Africa/Lagos", 6.5244, 3.3792),
        City("Nairobi", "Kenia", "Africa/Nairobi", -1.2921, 36.8219),
        City("Ciudad del Cabo", "Sudáfrica", "Africa/Johannesburg", -33.9249, 18.4241),
        City("Dubái", "EAU", "Asia/Dubai", 25.2048, 55.2708),
        City("Bombay", "India", "Asia/Kolkata", 19.0760, 72.8777),
        City("Delhi", "India", "Asia/Kolkata", 28.6139, 77.2090),
        City("Bangkok", "Tailandia", "Asia/Bangkok", 13.7563, 100.5018),
        City("Singapur", "Singapur", "Asia/Singapore", 1.3521, 103.8198),
        City("Hong Kong", "China", "Asia/Hong_Kong", 22.3193, 114.1694),
        City("Shanghái", "China", "Asia/Shanghai", 31.2304, 121.4737),
        City("Pekín", "China", "Asia/Shanghai", 39.9042, 116.4074),
        City("Seúl", "Corea del Sur", "Asia/Seoul", 37.5665, 126.9780),
        City("Tokio", "Japón", "Asia/Tokyo", 35.6762, 139.6503),
        City("Sídney", "Australia", "Australia/Sydney", -33.8688, 151.2093),
        City("Melbourne", "Australia", "Australia/Melbourne", -37.8136, 144.9631),
        City("Auckland", "Nueva Zelanda", "Pacific/Auckland", -36.8509, 174.7645),
        City("Honolulu", "EE. UU.", "Pacific/Honolulu", 21.3069, -157.8583),
        City("Los Ángeles", "EE. UU.", "America/Los_Angeles", 34.0522, -118.2437),
        City("San Francisco", "EE. UU.", "America/Los_Angeles", 37.7749, -122.4194),
        City("Vancouver", "Canadá", "America/Vancouver", 49.2827, -123.1207),
        City("Denver", "EE. UU.", "America/Denver", 39.7392, -104.9903),
        City("Ciudad de México", "México", "America/Mexico_City", 19.4326, -99.1332),
        City("Chicago", "EE. UU.", "America/Chicago", 41.8781, -87.6298),
        City("Nueva York", "EE. UU.", "America/New_York", 40.7128, -74.0060),
        City("Toronto", "Canadá", "America/Toronto", 43.6532, -79.3832),
        City("Miami", "EE. UU.", "America/New_York", 25.7617, -80.1918),
        City("Bogotá", "Colombia", "America/Bogota", 4.7110, -74.0721),
        City("Lima", "Perú", "America/Lima", -12.0464, -77.0428),
        City("Caracas", "Venezuela", "America/Caracas", 10.4806, -66.9036),
        City("Santiago de Chile", "Chile", "America/Santiago", -33.4489, -70.6693),
        City("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires", -34.6037, -58.3816),
        City("São Paulo", "Brasil", "America/Sao_Paulo", -23.5505, -46.6333),
        City("Río de Janeiro", "Brasil", "America/Sao_Paulo", -22.9068, -43.1729),
        City("Montevideo", "Uruguay", "America/Montevideo", -34.9011, -56.1645),
        City("La Habana", "Cuba", "America/Havana", 23.1136, -82.3666),
        City("Reikiavik", "Islandia", "Atlantic/Reykjavik", 64.1466, -21.9426),
    )

    /** Clave guardada para una ciudad: "zona|nombre". */
    fun key(c: City) = c.zone + "|" + c.name

    fun parse(key: String): Pair<String, String> {
        val i = key.indexOf('|')
        return if (i < 0) key to key.substringAfterLast('/').replace('_', ' ') else key.substring(0, i) to key.substring(i + 1)
    }
}
