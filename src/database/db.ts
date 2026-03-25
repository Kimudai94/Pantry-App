import SQLite from 'react-native-sqlite-storage';

SQLite.DEBUG(true);
SQLite.enablePromise(true);

let db: SQLite.Database | null = null;

export async function initializeDatabase() {
  try {
    db = await SQLite.openDatabase({
      name: 'pantry.db',
      location: 'default',
    });

    // Create products table
    await db.executeSql(`
      CREATE TABLE IF NOT EXISTS products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        quantity REAL NOT NULL,
        unit TEXT DEFAULT 'g',
        mhd TEXT,
        price REAL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      );
    `);

    console.log('Database initialized successfully');
  } catch (error) {
    console.error('Error initializing database:', error);
    throw error;
  }
}

export async function getDatabase(): Promise<SQLite.Database> {
  if (!db) {
    await initializeDatabase();
  }
  return db!;
}

// Products CRUD operations

export async function getAllProducts() {
  const database = await getDatabase();
  const result = await database.executeSql(
    'SELECT * FROM products ORDER BY mhd ASC, name ASC'
  );
  return result[0]?.rows?._array || [];
}

export async function getProductById(id: number) {
  const database = await getDatabase();
  const result = await database.executeSql('SELECT * FROM products WHERE id = ?', [id]);
  return result[0]?.rows?._array?.[0] || null;
}

export async function addProduct(
  name: string,
  quantity: number,
  unit: string = 'g',
  mhd?: string,
  price?: number
) {
  const database = await getDatabase();
  const result = await database.executeSql(
    'INSERT INTO products (name, quantity, unit, mhd, price) VALUES (?, ?, ?, ?, ?)',
    [name, quantity, unit, mhd || null, price || null]
  );
  return result[1];
}

export async function updateProduct(
  id: number,
  name: string,
  quantity: number,
  unit: string = 'g',
  mhd?: string,
  price?: number
) {
  const database = await getDatabase();
  const result = await database.executeSql(
    'UPDATE products SET name = ?, quantity = ?, unit = ?, mhd = ?, price = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
    [name, quantity, unit, mhd || null, price || null, id]
  );
  return result[1];
}

export async function deleteProduct(id: number) {
  const database = await getDatabase();
  const result = await database.executeSql('DELETE FROM products WHERE id = ?', [id]);
  return result[1];
}

export async function closeDatabase() {
  if (db) {
    await db.close();
    db = null;
  }
}
