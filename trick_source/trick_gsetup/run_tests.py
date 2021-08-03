#!/usr/bin/env python3
import pytest
import sys

if __name__ == "__main__":
    args = list(sys.argv[1:]) + ["tests"]
    print("Runing tests with arguments:", args)
    sys.exit(pytest.main(args))