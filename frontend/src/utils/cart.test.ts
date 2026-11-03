import test from 'node:test';
import assert from 'node:assert/strict';
import { formatVND, calculateDiscount, calculateCartTotal } from './cart';

test('formatVND formats Vietnamese currency correctly with ₫ symbol', () => {
  assert.equal(formatVND(16990000), '16.990.000₫');
  assert.equal(formatVND(390000), '390.000₫');
  assert.equal(formatVND(0), '0₫');
});

test('calculateDiscount calculates integer percentage correctly', () => {
  assert.equal(calculateDiscount(18490000, 16990000), 8);
  assert.equal(calculateDiscount(27000000, 24790000), 8);
  assert.equal(calculateDiscount(100, 100), 0);
  assert.equal(calculateDiscount(100, 150), 0);
});

test('calculateCartTotal computes sum of items and counts', () => {
  const items = [
    { id: '1', name: 'PC GVN', price: 16990000, image: '', quantity: 1 },
    { id: '2', name: 'Chuột G102', price: 400000, image: '', quantity: 2 },
  ];
  const { subtotal, totalItems } = calculateCartTotal(items);
  assert.equal(subtotal, 16990000 + 400000 * 2);
  assert.equal(totalItems, 3);
});
