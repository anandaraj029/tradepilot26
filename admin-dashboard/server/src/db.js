import mongoose from 'mongoose';

let memoryServer = null;

/**
 * Connects to MONGODB_URI. When it is not set, starts an in-memory MongoDB
 * (dev only) so the dashboard can run without a local install.
 * Returns true when an in-memory database was started.
 */
export async function connectDb() {
  let uri = process.env.MONGODB_URI;
  let inMemory = false;
  if (!uri) {
    const { MongoMemoryServer } = await import('mongodb-memory-server');
    memoryServer = await MongoMemoryServer.create();
    uri = memoryServer.getUri('tradepilot_admin');
    inMemory = true;
    console.log('[db] MONGODB_URI not set - using in-memory MongoDB (data resets on restart)');
  }
  await mongoose.connect(uri);
  console.log(`[db] connected to ${inMemory ? 'in-memory MongoDB' : uri.replace(/\/\/.*@/, '//***@')}`);
  return inMemory;
}

export async function disconnectDb() {
  await mongoose.disconnect();
  if (memoryServer) await memoryServer.stop();
}
