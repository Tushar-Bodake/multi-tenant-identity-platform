package com.securegate.tenant.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

@Configuration
public class TenantDataSourceConfig implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
            return new TenantAwareDataSource(dataSource);
        }
        return bean;
    }

    public static class TenantAwareDataSource extends DelegatingDataSource {

        public TenantAwareDataSource(DataSource targetDataSource) {
            super(targetDataSource);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection connection = super.getConnection();
            applyTenantContext(connection);
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection connection = super.getConnection(username, password);
            applyTenantContext(connection);
            return connection;
        }

        private void applyTenantContext(Connection connection) {
            try {
                if (connection == null || connection.isClosed()) {
                    return;
                }
                String dbProduct = connection.getMetaData().getDatabaseProductName();
                if (dbProduct != null && dbProduct.equalsIgnoreCase("PostgreSQL")) {
                    UUID tenantId = TenantContext.getTenantId();
                    boolean isPlatformAdmin = TenantContext.isPlatformAdmin();
                    UUID userId = TenantContext.getUserId();
                    String userEmail = TenantContext.getUserEmail();

                    boolean isLocal = !connection.getAutoCommit();

                    try (PreparedStatement ps = connection.prepareStatement("SELECT set_config(?, ?, ?)")) {
                        ps.setString(1, "app.current_tenant_id");
                        ps.setString(2, tenantId != null ? tenantId.toString() : "");
                        ps.setBoolean(3, isLocal);
                        ps.execute();

                        ps.setString(1, "app.is_platform_admin");
                        ps.setString(2, isPlatformAdmin ? "true" : "false");
                        ps.setBoolean(3, isLocal);
                        ps.execute();

                        ps.setString(1, "app.current_user_id");
                        ps.setString(2, userId != null ? userId.toString() : "");
                        ps.setBoolean(3, isLocal);
                        ps.execute();

                        ps.setString(1, "app.current_user_email");
                        ps.setString(2, userEmail != null ? userEmail : "");
                        ps.setBoolean(3, isLocal);
                        ps.execute();
                    }
                }
            } catch (SQLException ignored) {
                // Ignore transient connection preparation exceptions during flyway or schema inspection
            }
        }
    }
}
