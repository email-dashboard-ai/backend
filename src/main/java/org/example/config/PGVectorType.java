package org.example.config;

import com.pgvector.PGvector;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Custom Hibernate UserType for pgvector PGvector type Handles conversion between Java PGvector
 * objects and PostgreSQL vector columns
 */
public class PGVectorType implements UserType<PGvector> {

  @Override
  public int getSqlType() {
    return Types.OTHER;
  }

  @Override
  public Class<PGvector> returnedClass() {
    return PGvector.class;
  }

  @Override
  public boolean equals(PGvector x, PGvector y) {
    if (x == y) return true;
    if (x == null || y == null) return false;
    return x.toString().equals(y.toString());
  }

  @Override
  public int hashCode(PGvector x) {
    return x == null ? 0 : x.hashCode();
  }

  @Override
  public PGvector nullSafeGet(
      ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    String value = rs.getString(position);
    if (value == null) {
      return null;
    }
    return new PGvector(value);
  }

  @Override
  public void nullSafeSet(
      PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session)
      throws SQLException {
    if (value == null) {
      st.setNull(index, Types.OTHER);
    } else {
      st.setObject(index, value);
    }
  }

  @Override
  public PGvector deepCopy(PGvector value) {
    if (value == null) return null;
    try {
      return new PGvector(value.toString());
    } catch (Exception e) {
      throw new RuntimeException("Error deep copying PGvector", e);
    }
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Serializable disassemble(PGvector value) {
    return value == null ? null : value.toString();
  }

  @Override
  public PGvector assemble(Serializable cached, Object owner) {
    if (cached == null) return null;
    try {
      return new PGvector((String) cached);
    } catch (Exception e) {
      throw new RuntimeException("Error assembling PGvector", e);
    }
  }

  @Override
  public PGvector replace(PGvector detached, PGvector managed, Object owner) {
    return deepCopy(detached);
  }
}
