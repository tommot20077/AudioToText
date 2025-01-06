import json
import sys
import traceback
from deepmultilingualpunctuation import PunctuationModel

print("啟動腳本中...", file=sys.stderr)
sys.stderr.flush()

try:
    from deepmultilingualpunctuation import PunctuationModel

    model = PunctuationModel()
    print("成功載入標點符號模型", file=sys.stderr)
except Exception as e:
    print(f"載入模型時發生錯誤: {str(e)}", file=sys.stderr)
    traceback.print_exc(file=sys.stderr)
    sys.exit(1)

sys.stderr.flush()


def main():
    taskId = "Unknown"
    print("開始啟動執行腳本", file=sys.stderr)
    sys.stderr.flush()

    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                print("空白輸入，不處理", file=sys.stderr)
                break

            print(f"收到處理句子: {line.strip()}", file=sys.stderr)
            sys.stderr.flush()

            data = json.loads(line)
            text = data.get("text", "")
            taskId = data.get("taskId", "")

            if text == "test" and taskId == "test-init":
                print(json.dumps({"isSuccess": True, "restoredText": "test", "taskId": "test-init"}))
                sys.stdout.flush()
                continue

            print(f"處理任務ID: {taskId}", file=sys.stderr)
            restored_text = restore_punctuation(text)
            output = {"isSuccess": True, "restoredText": restored_text, "taskId": taskId}

        except Exception as e:
            print(f"發生錯誤: {str(e)}", file=sys.stderr)
            traceback.print_exc(file=sys.stderr)
            output = {"isSuccess": False, "error": str(e), "taskId": taskId}

        print(json.dumps(output))
        sys.stdout.flush()
        print(f"發送處理結果，任務ID: {taskId}", file=sys.stderr)
        sys.stderr.flush()


def restore_punctuation(text):
    clean_text = model.preprocess(text)
    labeled_words = model.predict(clean_text)
    size = len(labeled_words)
    result = ""

    isNeedToUpperCase = True
    for i in range(size):
        word = labeled_words[i]
        if isNeedToUpperCase:
            word[0] = word[0].capitalize()
            isNeedToUpperCase = False

        result += word[0]
        if word[1] == "0":
            result += " "
        elif not str(word[1]).isdigit():
            if i is not size - 1:
                result += word[1] + " "

                if not word[1] == "," and not word[1] == "-" and not word[1] == "'":
                    isNeedToUpperCase = True
            else:
                result += word[1]
    return result


if __name__ == "__main__":
    main()
