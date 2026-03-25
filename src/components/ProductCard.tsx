import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { Product } from '../types/product';
import { differenceInDays, parseISO } from 'date-fns';

interface ProductCardProps {
  product: Product;
  onEdit: () => void;
  onDelete: () => void;
}

export default function ProductCard({ product, onEdit, onDelete }: ProductCardProps) {
  const getMhdStatus = () => {
    if (!product.mhd) {
      return { type: 'none', text: 'MHD nicht angegeben' };
    }

    try {
      const mhdDate = parseISO(product.mhd);
      const today = new Date();
      const days = differenceInDays(mhdDate, today);

      if (days < 0) {
        return { type: 'expired', text: `❌ Abgelaufen am ${product.mhd}` };
      } else if (days <= 7) {
        return { type: 'warning', text: `⚠️ MHD bald abgelaufen! (${days} Tage)` };
      } else {
        return { type: 'ok', text: `✅ MHD: ${product.mhd}` };
      }
    } catch {
      return { type: 'none', text: 'Ungültiges Datum' };
    }
  };

  const mhdStatus = getMhdStatus();

  return (
    <View style={styles.card}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.productName}>{product.name}</Text>
          <Text style={styles.quantity}>
            {product.quantity} {product.unit}
          </Text>
        </View>

        <View style={styles.details}>
          {product.price && (
            <Text style={styles.detail}>💰 {product.price.toFixed(2)} €</Text>
          )}
          <Text
            style={[
              styles.mhd,
              mhdStatus.type === 'expired' && styles.mhdExpired,
              mhdStatus.type === 'warning' && styles.mhdWarning,
            ]}
          >
            {mhdStatus.text}
          </Text>
        </View>
      </View>

      <View style={styles.actions}>
        <TouchableOpacity style={[styles.button, styles.editButton]} onPress={onEdit}>
          <Text style={styles.buttonText}>📝</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.button, styles.deleteButton]} onPress={onDelete}>
          <Text style={styles.buttonText}>🗑️</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 12,
    marginBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  content: {
    flex: 1,
    gap: 8,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  productName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  quantity: {
    fontSize: 14,
    fontWeight: '500',
    color: '#666',
    marginLeft: 8,
  },
  details: {
    gap: 4,
  },
  detail: {
    fontSize: 13,
    color: '#666',
  },
  mhd: {
    fontSize: 13,
    fontWeight: '500',
    color: '#27ae60',
  },
  mhdWarning: {
    color: '#f39c12',
  },
  mhdExpired: {
    color: '#e74c3c',
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
    marginLeft: 12,
  },
  button: {
    width: 40,
    height: 40,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  editButton: {
    backgroundColor: '#3498db',
  },
  deleteButton: {
    backgroundColor: '#e74c3c',
  },
  buttonText: {
    fontSize: 18,
  },
});
