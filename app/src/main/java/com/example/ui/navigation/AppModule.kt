package com.example.ui.navigation

enum class AppModule(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val tag: String
) {
    DASHBOARD("Dashboard", "Resumen financiero y estado", "📊", "nav_dashboard"),
    PROVEEDORES("Proveedores", "Gestión y catálogo de proveedores", "🚚", "nav_proveedores"),
    CLIENTES("Clientes", "Cartera y cuentas de clientes", "👥", "nav_clientes"),
    COMPRAS("Compras", "Registro de compras e insumos", "📥", "nav_compras"),
    INVENTARIO("Inventario", "Control de stock, insumos y mermas", "📦", "nav_inventario"),
    VENTAS("Ventas", "Punto de venta y facturación", "🛍️", "nav_ventas"),
    CXC("CXC", "Cuentas por cobrar a clientes", "💰", "nav_cxc"),
    CXP("CXP", "Cuentas por pagar a proveedores", "📑", "nav_cxp"),
    REPORTES("Reportes", "Balance, utilidades y estadísticas", "📈", "nav_reportes"),
    AJUSTES("Ajustes", "Configuración del negocio y menú", "⚙️", "nav_ajustes")
}
