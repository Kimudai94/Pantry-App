import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { getProductById, updateProduct } from '../database/db';
import { Product } from '../types/product';

const UNITS = ['g', 'ml', 'l', 'kg', 'stck', 'pack'];

export default function EditProductScreen({ route, navigation }: any) {
  const { id } = route.params;

  const [product, setProduct] = useState<Product | null>(null);
  const [name, setName] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unit, setUnit] = useState('g');
  const [mhd, setMhd] = useState('');
  const [price, setPrice] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadProduct();
  }, []);

  const loadProduct = async () => {
    try {
      const data = await getProductById(id);
      if (data) {
        setProduct(data);
        setName(data.name);
        setQuantity(data.quantity.toString());
        setUnit(data.unit);
        setMhd(data.mhd || '');
        setPrice(data.price?.toString() || '');
      }
    } catch (error) {
      Alert.alert('Fehler', 'Produkt konnte nicht geladen werden');
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!name.trim() || !quantity.trim()) {
      Alert.alert('Fehler', 'Bitte füllen Sie Name und Menge aus!');
      return;
    }

    try {
      setSaving(true);
      await updateProduct(
        id,
        name.trim(),
        parseFloat(quantity),
        unit,
        mhd || undefined,
        price ? parseFloat(price) : undefined
      );
      Alert.alert('Erfolg', 'Produkt aktualisiert!');
      navigation.goBack();
    } catch (error) {
      Alert.alert('Fehler', 'Produkt konnte nicht aktualisiert werden');
      console.error(error);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#2ecc71" />
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.form}>
        {/* Product Name */}
        <View style={styles.formGroup}>
          <Text style={styles.label}>Produktname *</Text>
          <TextInput
            style={styles.input}
            placeholder="z.B. Milch"
            value={name}
            onChangeText={setName}
            editable={!saving}
          />
        </View>

        {/* Quantity and Unit */}
        <View style={styles.row}>
          <View style={[styles.formGroup, { flex: 1, marginRight: 8 }]}>
            <Text style={styles.label}>Menge *</Text>
            <TextInput
              style={styles.input}
              placeholder="100"
              value={quantity}
              onChangeText={setQuantity}
              keyboardType="decimal-pad"
              editable={!saving}
            />
          </View>
          <View style={[styles.formGroup, { flex: 1 }]}>
            <Text style={styles.label}>Einheit</Text>
            <View style={styles.pickerContainer}>
              <Picker
                selectedValue={unit}
                onValueChange={setUnit}
                enabled={!saving}
              >
                {UNITS.map((u) => (
                  <Picker.Item key={u} label={u} value={u} />
                ))}
              </Picker>
            </View>
          </View>
        </View>

        {/* MHD and Price */}
        <View style={styles.row}>
          <View style={[styles.formGroup, { flex: 1, marginRight: 8 }]}>
            <Text style={styles.label}>MHD (YYYY-MM-DD)</Text>
            <TextInput
              style={styles.input}
              placeholder="2024-04-01"
              value={mhd}
              onChangeText={setMhd}
              editable={!saving}
            />
          </View>
          <View style={[styles.formGroup, { flex: 1 }]}>
            <Text style={styles.label}>Preis (€)</Text>
            <TextInput
              style={styles.input}
              placeholder="0.00"
              value={price}
              onChangeText={setPrice}
              keyboardType="decimal-pad"
              editable={!saving}
            />
          </View>
        </View>

        {/* Buttons */}
        <View style={styles.buttonContainer}>
          <TouchableOpacity
            style={[styles.button, styles.buttonPrimary, saving && styles.buttonDisabled]}
            onPress={handleUpdate}
            disabled={saving}
          >
            {saving ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.buttonText}>Speichern</Text>
            )}
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.buttonSecondary, saving && styles.buttonDisabled]}
            onPress={() => navigation.goBack()}
            disabled={saving}
          >
            <Text style={styles.buttonSecondaryText}>Abbrechen</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    padding: 16,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  form: {
    gap: 16,
  },
  formGroup: {
    gap: 8,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  input: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  row: {
    flexDirection: 'row',
  },
  pickerContainer: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    overflow: 'hidden',
  },
  buttonContainer: {
    gap: 8,
    marginTop: 8,
  },
  button: {
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonPrimary: {
    backgroundColor: '#2ecc71',
  },
  buttonSecondary: {
    backgroundColor: '#e8e8e8',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  buttonSecondaryText: {
    color: '#333',
    fontSize: 16,
    fontWeight: '600',
  },
  buttonDisabled: {
    opacity: 0.6,
  },
});
