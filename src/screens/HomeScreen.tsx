import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  TextInput,
  ActivityIndicator,
} from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { getAllProducts, deleteProduct } from '../database/db';
import ProductCard from '../components/ProductCard';
import { Product } from '../types/product';
import { isBefore, isWithin, today, differenceInDays } from 'date-fns';

export default function HomeScreen({ navigation }: any) {
  const [products, setProducts] = useState<Product[]>([]);
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [searchText, setSearchText] = useState('');
  const [sortType, setSortType] = useState<'name' | 'mhd' | 'price'>('mhd');
  const [loading, setLoading] = useState(true);

  // Load products when screen is focused
  useFocusEffect(
    useCallback(() => {
      loadProducts();
    }, [])
  );

  const loadProducts = async () => {
    try {
      setLoading(true);
      const data = await getAllProducts();
      setProducts(data);
      filterAndSort(data, searchText, sortType);
    } catch (error) {
      Alert.alert('Fehler', 'Produkte konnten nicht geladen werden');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const filterAndSort = (
    items: Product[],
    search: string,
    sort: 'name' | 'mhd' | 'price'
  ) => {
    let filtered = items.filter((product) =>
      product.name.toLowerCase().includes(search.toLowerCase())
    );

    filtered.sort((a, b) => {
      switch (sort) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'mhd':
          if (!a.mhd) return 1;
          if (!b.mhd) return -1;
          return new Date(a.mhd).getTime() - new Date(b.mhd).getTime();
        case 'price':
          return (b.price || 0) - (a.price || 0);
        default:
          return 0;
      }
    });

    setFilteredProducts(filtered);
  };

  const handleSearch = (text: string) => {
    setSearchText(text);
    filterAndSort(products, text, sortType);
  };

  const handleSort = (sort: 'name' | 'mhd' | 'price') => {
    setSortType(sort);
    filterAndSort(products, searchText, sort);
  };

  const handleDelete = (id: number, name: string) => {
    Alert.alert(
      'Produkt löschen',
      `Möchtest du "${name}" wirklich löschen?`,
      [
        { text: 'Abbrechen', onPress: () => {} },
        {
          text: 'Löschen',
          onPress: async () => {
            try {
              await deleteProduct(id);
              loadProducts();
            } catch (error) {
              Alert.alert('Fehler', 'Produkt konnte nicht gelöscht werden');
            }
          },
          style: 'destructive',
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      {/* Search Bar */}
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="Produkte durchsuchen..."
          value={searchText}
          onChangeText={handleSearch}
        />
      </View>

      {/* Sort Buttons */}
      <View style={styles.sortContainer}>
        <TouchableOpacity
          style={[styles.sortBtn, sortType === 'name' && styles.sortBtnActive]}
          onPress={() => handleSort('name')}
        >
          <Text style={[styles.sortBtnText, sortType === 'name' && styles.sortBtnTextActive]}>
            Name
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.sortBtn, sortType === 'mhd' && styles.sortBtnActive]}
          onPress={() => handleSort('mhd')}
        >
          <Text style={[styles.sortBtnText, sortType === 'mhd' && styles.sortBtnTextActive]}>
            MHD
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.sortBtn, sortType === 'price' && styles.sortBtnActive]}
          onPress={() => handleSort('price')}
        >
          <Text style={[styles.sortBtnText, sortType === 'price' && styles.sortBtnTextActive]}>
            Preis
          </Text>
        </TouchableOpacity>
      </View>

      {/* Products List */}
      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#2ecc71" />
        </View>
      ) : filteredProducts.length === 0 ? (
        <View style={styles.centerContainer}>
          <Text style={styles.emptyText}>
            {products.length === 0
              ? 'Noch keine Produkte. Tippe auf + um eines hinzuzufügen!'
              : 'Keine Produkte gefunden.'}
          </Text>
        </View>
      ) : (
        <FlatList
          data={filteredProducts}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => (
            <ProductCard
              product={item}
              onEdit={() => navigation.navigate('EditProduct', { id: item.id })}
              onDelete={() => handleDelete(item.id, item.name)}
            />
          )}
          contentContainerStyle={styles.listContent}
        />
      )}

      {/* Floating Action Button */}
      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('AddProduct')}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  searchContainer: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  searchInput: {
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 16,
  },
  sortContainer: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 12,
    backgroundColor: '#fff',
    gap: 8,
  },
  sortBtn: {
    flex: 1,
    paddingVertical: 8,
    borderRadius: 6,
    backgroundColor: '#f5f5f5',
    alignItems: 'center',
  },
  sortBtnActive: {
    backgroundColor: '#2ecc71',
  },
  sortBtnText: {
    color: '#666',
    fontWeight: '600',
    fontSize: 12,
  },
  sortBtnTextActive: {
    color: '#fff',
  },
  listContent: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 12,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    textAlign: 'center',
  },
  fab: {
    position: 'absolute',
    bottom: 16,
    right: 16,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#2ecc71',
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  fabText: {
    fontSize: 32,
    color: '#fff',
    fontWeight: 'bold',
  },
});
