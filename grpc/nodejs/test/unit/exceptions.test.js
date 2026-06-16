/**
 * Unit tests for the exception hierarchy.
 */

const {
  FreeplaneGrpcError,
  FreeplaneConnectionError,
  FreeplaneOperationError,
  NodeNotFoundError,
  MindMapError,
} = require('../../src/exceptions');

describe('FreeplaneGrpcError', () => {
  test('is an Error subclass', () => {
    const err = new FreeplaneGrpcError('test');
    expect(err).toBeInstanceOf(Error);
    expect(err).toBeInstanceOf(FreeplaneGrpcError);
    expect(err.message).toBe('test');
    expect(err.name).toBe('FreeplaneGrpcError');
  });
});

describe('FreeplaneConnectionError', () => {
  test('extends FreeplaneGrpcError', () => {
    const err = new FreeplaneConnectionError('conn failed');
    expect(err).toBeInstanceOf(FreeplaneGrpcError);
    expect(err).toBeInstanceOf(FreeplaneConnectionError);
    expect(err.message).toBe('conn failed');
    expect(err.name).toBe('FreeplaneConnectionError');
  });

  test('does not extend FreeplaneOperationError', () => {
    const err = new FreeplaneConnectionError('conn failed');
    expect(err).not.toBeInstanceOf(FreeplaneOperationError);
  });
});

describe('FreeplaneOperationError', () => {
  test('extends FreeplaneGrpcError', () => {
    const err = new FreeplaneOperationError('op failed');
    expect(err).toBeInstanceOf(FreeplaneGrpcError);
    expect(err).toBeInstanceOf(FreeplaneOperationError);
    expect(err.message).toBe('op failed');
    expect(err.name).toBe('FreeplaneOperationError');
  });
});

describe('NodeNotFoundError', () => {
  test('extends FreeplaneOperationError', () => {
    const err = new NodeNotFoundError('node not found');
    expect(err).toBeInstanceOf(FreeplaneOperationError);
    expect(err).toBeInstanceOf(FreeplaneGrpcError);
    expect(err).toBeInstanceOf(NodeNotFoundError);
    expect(err.message).toBe('node not found');
    expect(err.name).toBe('NodeNotFoundError');
  });
});

describe('MindMapError', () => {
  test('extends FreeplaneOperationError', () => {
    const err = new MindMapError('map error');
    expect(err).toBeInstanceOf(FreeplaneOperationError);
    expect(err).toBeInstanceOf(FreeplaneGrpcError);
    expect(err).toBeInstanceOf(MindMapError);
    expect(err.message).toBe('map error');
    expect(err.name).toBe('MindMapError');
  });
});

describe('exception hierarchy completeness', () => {
  test('all exceptions are Error instances', () => {
    const errors = [
      new FreeplaneGrpcError('a'),
      new FreeplaneConnectionError('b'),
      new FreeplaneOperationError('c'),
      new NodeNotFoundError('d'),
      new MindMapError('e'),
    ];
    errors.forEach(err => {
      expect(err).toBeInstanceOf(Error);
      expect(err).toHaveProperty('message');
      expect(err).toHaveProperty('name');
    });
  });

  test('can catch base class for all derived types', () => {
    const errors = [
      new FreeplaneConnectionError('a'),
      new FreeplaneOperationError('b'),
      new NodeNotFoundError('c'),
      new MindMapError('d'),
    ];
    errors.forEach(err => {
      expect(() => {
        if (err instanceof FreeplaneGrpcError) throw err;
      }).toThrow(FreeplaneGrpcError);
    });
  });
});
